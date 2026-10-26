package com.example.reliability;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class PostConsumer {

    private static final Logger log = LoggerFactory.getLogger(PostConsumer.class);

    private final PostRepository repo;

    public PostConsumer(PostRepository repo) {
        this.repo = repo;
    }

    @KafkaListener(topics = PostTopicConfig.POSTS_TOPIC, groupId = "post-publisher")
    public void publish(ConsumerRecord<String, String> record) {
        Post post = repo.findById(Long.valueOf(record.key())).orElse(null);
        if (post == null) {
            log.warn("Post {} not found, skipping", record.key());
            return;
        }
        if (post.getStatus() == PostStatus.PUBLISHED) {          // idempotency
            log.warn("DUPLICATE: post {} already published, ignored", post.getId());
            return;
        }
        if (post.getContent().contains("FAIL")) {                // simulate the platform rejecting the post
            log.error("Platform rejected post {}", post.getId());
            throw new IllegalStateException("Platform rejected post " + post.getId());
        }

        ThreadLocalRandom r = ThreadLocalRandom.current();       // simulated engagement metrics
        int impressions = r.nextInt(200, 5000);
        int likes = r.nextInt(0, impressions / 10 + 1);
        post.setImpressions(impressions);
        post.setLikes(likes);
        post.setShares(r.nextInt(0, likes / 2 + 1));
        post.setStatus(PostStatus.PUBLISHED);
        post.setPublishedAt(Instant.now());
        repo.save(post);
        log.info("SUCCESS: post {} published", post.getId());
    }

    // After retries are exhausted the event lands in the DLQ; mark the post FAILED
    @KafkaListener(topics = KafkaConfig.DLQ_TOPIC, groupId = "post-dlq-group")
    public void onDeadLetter(ConsumerRecord<String, String> record) {
        Header origin = record.headers().lastHeader(KafkaHeaders.DLT_ORIGINAL_TOPIC);
        if (origin == null || !PostTopicConfig.POSTS_TOPIC.equals(new String(origin.value(), StandardCharsets.UTF_8))) {
            return;                                              // not one of ours (e.g. an order event)
        }
        repo.findById(Long.valueOf(record.key())).ifPresent(post -> {
            post.setStatus(PostStatus.FAILED);
            repo.save(post);
            log.error("Post {} moved to FAILED after retries (DLQ)", post.getId());
        });
    }
}