package com.example.reliability;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostConsumerTest {

    @Mock PostRepository repo;
    PostConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new PostConsumer(repo);
    }

    private Post post(String content, PostStatus status) {
        Post p = new Post(1L, 2L, "a@b.com", content, "TWITTER");
        p.setStatus(status);
        return p;
    }

    private ConsumerRecord<String, String> record(String key, String value) {
        return new ConsumerRecord<>("posts-topic", 0, 0L, key, value);
    }

    @Test
    void publishesQueuedPostAndRecordsMetrics() {
        Post p = post("hello", PostStatus.QUEUED);
        when(repo.findById(5L)).thenReturn(Optional.of(p));

        consumer.publish(record("5", "hello"));

        assertEquals(PostStatus.PUBLISHED, p.getStatus());
        assertNotNull(p.getPublishedAt());
        assertTrue(p.getImpressions() >= 200);
        verify(repo).save(p);
    }

    @Test
    void alreadyPublishedPostIsIgnored() {
        when(repo.findById(5L)).thenReturn(Optional.of(post("hello", PostStatus.PUBLISHED)));
        consumer.publish(record("5", "hello"));
        verify(repo, never()).save(any());
    }

    @Test
    void unknownPostIsSkipped() {
        when(repo.findById(9L)).thenReturn(Optional.empty());
        consumer.publish(record("9", "hello"));
        verify(repo, never()).save(any());
    }

    @Test
    void rejectedContentThrowsSoTheRetryAndDlqFlowStarts() {
        when(repo.findById(5L)).thenReturn(Optional.of(post("this will FAIL", PostStatus.QUEUED)));
        assertThrows(IllegalStateException.class, () -> consumer.publish(record("5", "this will FAIL")));
        verify(repo, never()).save(any());
    }

    @Test
    void deadLetterFromPostsTopicMarksPostFailed() {
        Post p = post("FAIL", PostStatus.QUEUED);
        when(repo.findById(5L)).thenReturn(Optional.of(p));
        ConsumerRecord<String, String> r = record("5", "FAIL");
        r.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, "posts-topic".getBytes(StandardCharsets.UTF_8));

        consumer.onDeadLetter(r);

        assertEquals(PostStatus.FAILED, p.getStatus());
        verify(repo).save(p);
    }

    @Test
    void deadLetterFromOtherTopicIsIgnored() {
        ConsumerRecord<String, String> r = record("5", "x");
        r.headers().add(KafkaHeaders.DLT_ORIGINAL_TOPIC, "orders-topic".getBytes(StandardCharsets.UTF_8));
        consumer.onDeadLetter(r);
        verifyNoInteractions(repo);
    }

    @Test
    void deadLetterWithoutHeaderIsIgnored() {
        consumer.onDeadLetter(record("5", "x"));
        verifyNoInteractions(repo);
    }
}