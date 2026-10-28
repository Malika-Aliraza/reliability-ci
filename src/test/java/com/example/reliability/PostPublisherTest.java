package com.example.reliability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostPublisherTest {

    @Mock PostRepository repo;
    @Mock KafkaTemplate<String, String> kafka;

    private Post post(long id, String content) {
        Post p = new Post(1L, 1L, "a@b.com", content, "TWITTER");
        ReflectionTestUtils.setField(p, "id", id);
        return p;
    }

    @Test
    void onlyClaimedPostsAreSentToKafka() {
        when(repo.findByStatusAndScheduledAtLessThanEqual(eq(PostStatus.SCHEDULED), any(Instant.class)))
                .thenReturn(List.of(post(1L, "first"), post(2L, "second")));
        when(repo.claim(1L, PostStatus.SCHEDULED, PostStatus.QUEUED)).thenReturn(1);
        when(repo.claim(2L, PostStatus.SCHEDULED, PostStatus.QUEUED)).thenReturn(0); // another instance won

        new PostPublisher(repo, kafka).publishDuePosts();

        verify(kafka).send("posts-topic", "1", "first");
        verify(kafka, never()).send("posts-topic", "2", "second");
    }

    @Test
    void nothingIsSentWhenNoPostIsDue() {
        when(repo.findByStatusAndScheduledAtLessThanEqual(eq(PostStatus.SCHEDULED), any(Instant.class)))
                .thenReturn(List.of());
        new PostPublisher(repo, kafka).publishDuePosts();
        verifyNoInteractions(kafka);
    }
}