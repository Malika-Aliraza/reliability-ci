package com.example.reliability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
class KafkaIntegrationTest {

    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("apache/kafka:3.7.0"));

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired KafkaTemplate<String, String> kafkaTemplate;
    @Autowired ProcessedEventRepository processedRepo;
    @Autowired DeadLetterEventRepository dlqRepo;
    @Autowired WebApplicationContext wac;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void successfulEventIsProcessedAndVisibleThroughApi() throws Exception {
        kafkaTemplate.send("orders-topic", "201", "hello");

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertTrue(processedRepo.existsById("201")));

        mockMvc.perform(get("/processed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.eventId=='201')]").exists());
    }

    @Test
    void duplicateEventIsProcessedOnlyOnce() {
        kafkaTemplate.send("orders-topic", "202", "hello");
        kafkaTemplate.send("orders-topic", "202", "hello");

        await().atMost(Duration.ofSeconds(20))
                .untilAsserted(() -> assertTrue(processedRepo.existsById("202")));

        long count = processedRepo.findAll().stream()
                .filter(e -> e.getEventId().equals("202")).count();
        assertEquals(1, count);
    }

    @Test
    void poisonEventIsRetriedThenSentToDlq() {
        kafkaTemplate.send("orders-topic", "203", "FAIL");

        await().atMost(Duration.ofSeconds(40))
                .untilAsserted(() -> assertTrue(dlqRepo.findFirstByEventKeyOrderByIdDesc("203").isPresent()));

        assertTrue(processedRepo.findById("203").isEmpty());
    }
}