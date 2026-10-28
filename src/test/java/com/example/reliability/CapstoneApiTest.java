package com.example.reliability;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:capstone;DB_CLOSE_DELAY=-1",
        "spring.kafka.listener.auto-startup=false",
        "spring.kafka.admin.auto-create=false",
        "app.scheduler.delay-ms=3600000",
        "app.scheduler.initial-delay-ms=3600000"
})
class CapstoneApiTest {

    @TestConfiguration
    static class MockKafkaConfig {
        @Bean
        @SuppressWarnings("unchecked")
        KafkaTemplate<String, String> kafkaTemplate() {
            return Mockito.mock(KafkaTemplate.class);
        }
    }

    private static final String JSON = MediaType.APPLICATION_JSON_VALUE;

    @Autowired WebApplicationContext wac;
    @Autowired @Qualifier("springSecurityFilterChain") Filter securityFilterChain;
    @Autowired PostRepository postRepo;
    @Autowired PostPublisher publisher;
    @Autowired KafkaTemplate<String, String> kafkaTemplate;

    MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(wac).addFilters(securityFilterChain).build();
    }

    // ---------------- helpers ----------------

    private String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@test.com";
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String register(String email) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.token");
    }

    private long personalWorkspace(String token) throws Exception {
        MvcResult r = mvc.perform(get("/api/workspaces").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$[0].id");
        return id.longValue();
    }

    private String postBody(String content, String platform, Instant when) {
        String scheduled = when == null ? "null" : "\"" + when + "\"";
        return "{\"content\":\"" + content + "\",\"platform\":\"" + platform + "\",\"scheduledAt\":" + scheduled + "}";
    }

    private long createPostOk(String token, long wid, String content, Instant when) throws Exception {
        MvcResult r = mvc.perform(post("/api/workspaces/" + wid + "/posts")
                        .header("Authorization", bearer(token)).contentType(JSON)
                        .content(postBody(content, "TWITTER", when)))
                .andExpect(status().isCreated()).andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private long memberIdOf(String token, long wid, String email) throws Exception {
        MvcResult r = mvc.perform(get("/api/workspaces/" + wid + "/members").header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        List<Number> ids = JsonPath.read(r.getResponse().getContentAsString(),
                "$[?(@.email=='" + email + "')].userId");
        return ids.get(0).longValue();
    }

    // ---------------- authentication ----------------

    @Test
    void registrationLoginAndTokenProtection() throws Exception {
        mvc.perform(get("/api/workspaces")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/workspaces").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());

        String email = uniqueEmail();
        String token = register(email);

        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"secret123\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType(JSON)
                        .content("{\"email\":\"" + uniqueEmail() + "\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"secret123\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.token").exists());
        mvc.perform(post("/api/auth/login").contentType(JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/workspaces").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].role").value("ADMIN"));
    }

    // ---------------- post lifecycle ----------------

    @Test
    void adminCanManagePostLifecycle() throws Exception {
        String token = register(uniqueEmail());
        long wid = personalWorkspace(token);
        String base = "/api/workspaces/" + wid + "/posts";

        long draftId = createPostOk(token, wid, "My draft", null);
        createPostOk(token, wid, "Scheduled one", Instant.now().plusSeconds(3600));

        for (String invalid : List.of(
                postBody("", "TWITTER", null),
                postBody("x".repeat(281), "TWITTER", null),
                postBody("hello", "MYSPACE", null),
                postBody("hello", "TWITTER", Instant.now().minusSeconds(60)))) {
            mvc.perform(post(base).header("Authorization", bearer(token)).contentType(JSON).content(invalid))
                    .andExpect(status().isBadRequest());
        }

        mvc.perform(get(base).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mvc.perform(put(base + "/" + draftId + "/schedule").header("Authorization", bearer(token))
                        .contentType(JSON).content("{\"scheduledAt\":\"" + Instant.now().plusSeconds(7200) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SCHEDULED"));
        mvc.perform(put(base + "/" + draftId + "/schedule").header("Authorization", bearer(token))
                        .contentType(JSON).content("{\"scheduledAt\":null}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(base + "/" + draftId + "/schedule").header("Authorization", bearer(token))
                        .contentType(JSON).content("{\"scheduledAt\":\"" + Instant.now().minusSeconds(60) + "\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(put(base + "/999999/schedule").header("Authorization", bearer(token))
                        .contentType(JSON).content("{\"scheduledAt\":\"" + Instant.now().plusSeconds(60) + "\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(delete(base + "/" + draftId).header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
        mvc.perform(delete(base + "/" + draftId).header("Authorization", bearer(token)))
                .andExpect(status().isNotFound());
    }

    // ---------------- role-based access control ----------------

    @Test
    void rolesAreEnforcedPerWorkspace() throws Exception {
        String adminToken = register(uniqueEmail());
        String memberEmail = uniqueEmail();
        String memberToken = register(memberEmail);
        String outsiderToken = register(uniqueEmail());
        long wid = personalWorkspace(adminToken);
        String base = "/api/workspaces/" + wid;
        long adminPostId = createPostOk(adminToken, wid, "Admin post", null);

        // a non-member cannot even see that the workspace exists
        mvc.perform(get(base + "/posts").header("Authorization", bearer(outsiderToken)))
                .andExpect(status().isNotFound());

        // admin adds the member as VIEWER
        mvc.perform(post(base + "/members").header("Authorization", bearer(adminToken)).contentType(JSON)
                        .content("{\"email\":\"" + memberEmail + "\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isCreated());

        // VIEWER: read-only
        mvc.perform(get(base + "/posts").header("Authorization", bearer(memberToken))).andExpect(status().isOk());
        mvc.perform(get(base + "/analytics").header("Authorization", bearer(memberToken))).andExpect(status().isOk());
        mvc.perform(post(base + "/posts").header("Authorization", bearer(memberToken)).contentType(JSON)
                        .content(postBody("nope", "TWITTER", null)))
                .andExpect(status().isForbidden());
        mvc.perform(post(base + "/members").header("Authorization", bearer(memberToken)).contentType(JSON)
                        .content("{\"email\":\"x@y.z\",\"role\":\"VIEWER\"}"))
                .andExpect(status().isForbidden());

        // promote to EDITOR
        long memberId = memberIdOf(adminToken, wid, memberEmail);
        mvc.perform(put(base + "/members/" + memberId).header("Authorization", bearer(adminToken))
                        .contentType(JSON).content("{\"role\":\"EDITOR\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("EDITOR"));

        // EDITOR: may write, may delete only own posts
        long ownPostId = createPostOk(memberToken, wid, "Editor post", null);
        mvc.perform(delete(base + "/posts/" + adminPostId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());
        mvc.perform(delete(base + "/posts/" + ownPostId).header("Authorization", bearer(memberToken)))
                .andExpect(status().isNoContent());

        // ADMIN may delete anyone's post
        long editorPost2 = createPostOk(memberToken, wid, "Another editor post", null);
        mvc.perform(delete(base + "/posts/" + editorPost2).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
    }

    @Test
    void memberManagementRulesAreEnforced() throws Exception {
        String adminEmail = uniqueEmail();
        String adminToken = register(adminEmail);
        String otherEmail = uniqueEmail();
        register(otherEmail);
        long wid = personalWorkspace(adminToken);
        String base = "/api/workspaces/" + wid + "/members";

        mvc.perform(post(base).header("Authorization", bearer(adminToken)).contentType(JSON)
                        .content("{\"email\":\"nobody-" + UUID.randomUUID() + "@test.com\",\"role\":\"EDITOR\"}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(base).header("Authorization", bearer(adminToken)).contentType(JSON)
                        .content("{\"email\":\"" + otherEmail + "\",\"role\":null}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(base).header("Authorization", bearer(adminToken)).contentType(JSON)
                        .content("{\"email\":\"" + otherEmail + "\",\"role\":\"EDITOR\"}"))
                .andExpect(status().isCreated());
        mvc.perform(post(base).header("Authorization", bearer(adminToken)).contentType(JSON)
                        .content("{\"email\":\"" + otherEmail + "\",\"role\":\"EDITOR\"}"))
                .andExpect(status().isConflict());

        // the last admin can be neither demoted nor removed
        long adminId = memberIdOf(adminToken, wid, adminEmail);
        mvc.perform(put(base + "/" + adminId).header("Authorization", bearer(adminToken))
                        .contentType(JSON).content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isConflict());
        mvc.perform(delete(base + "/" + adminId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isConflict());
        mvc.perform(put(base + "/" + adminId).header("Authorization", bearer(adminToken))
                        .contentType(JSON).content("{\"role\":null}"))
                .andExpect(status().isBadRequest());

        long otherId = memberIdOf(adminToken, wid, otherEmail);
        mvc.perform(delete(base + "/" + otherId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNoContent());
        mvc.perform(delete(base + "/" + otherId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());
        mvc.perform(put(base + "/" + otherId).header("Authorization", bearer(adminToken))
                        .contentType(JSON).content("{\"role\":\"VIEWER\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get(base).header("Authorization", bearer(adminToken)))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void userCanCreateAnAdditionalWorkspace() throws Exception {
        String token = register(uniqueEmail());

        mvc.perform(post("/api/workspaces").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"name\":\"Marketing team\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(post("/api/workspaces").header("Authorization", bearer(token)).contentType(JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/workspaces").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------------- analytics ----------------

    @Test
    void analyticsSummarisesWorkspacePosts() throws Exception {
        String token = register(uniqueEmail());
        long wid = personalWorkspace(token);
        String url = "/api/workspaces/" + wid + "/analytics";

        mvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPosts").value(0))
                .andExpect(jsonPath("$.engagementRate").value(0.0));

        long firstId = createPostOk(token, wid, "one", null);
        createPostOk(token, wid, "two", null);
        Post published = postRepo.findById(firstId).orElseThrow();
        published.setStatus(PostStatus.PUBLISHED);
        published.setImpressions(1000);
        published.setLikes(50);
        published.setShares(50);
        postRepo.save(published);

        mvc.perform(get(url).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPosts").value(2))
                .andExpect(jsonPath("$.impressions").value(1000))
                .andExpect(jsonPath("$.engagementRate").value(10.0))
                .andExpect(jsonPath("$.byStatus.PUBLISHED").value(1))
                .andExpect(jsonPath("$.byStatus.DRAFT").value(1))
                .andExpect(jsonPath("$.byPlatform.TWITTER").value(2));
    }

    // ---------------- Kafka publishing ----------------

    @Test
    void schedulerQueuesDuePostsToKafkaExactlyOnce() throws Exception {
        String token = register(uniqueEmail());
        long wid = personalWorkspace(token);
        long id = createPostOk(token, wid, "scheduler content", Instant.now().plusSeconds(3600));

        Post post = postRepo.findById(id).orElseThrow();
        post.setScheduledAt(Instant.now().minusSeconds(5));   // make it due
        postRepo.save(post);

        publisher.publishDuePosts();
        publisher.publishDuePosts();                           // second run must not send it again

        assertEquals(PostStatus.QUEUED, postRepo.findById(id).orElseThrow().getStatus());
        verify(kafkaTemplate).send("posts-topic", String.valueOf(id), "scheduler content");
    }

    // ---------------- open endpoints and documentation ----------------

    @Test
    void earlierExperimentEndpointsAndSwaggerDocsStayAvailable() throws Exception {
        mvc.perform(get("/version")).andExpect(status().isOk());
        mvc.perform(get("/send").param("id", "1").param("payload", "hello")).andExpect(status().isOk());
        mvc.perform(get("/processed")).andExpect(status().isOk());
        mvc.perform(get("/dlq")).andExpect(status().isOk());
        mvc.perform(get("/replay").param("id", "missing").param("payload", "x")).andExpect(status().isOk());

        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists());
    }
}