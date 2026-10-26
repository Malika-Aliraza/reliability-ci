package com.example.reliability;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class PostPublisher {

    private static final Logger log = LoggerFactory.getLogger(PostPublisher.class);

    private final PostRepository repo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public PostPublisher(PostRepository repo, KafkaTemplate<String, String> kafkaTemplate) {
        this.repo = repo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelay = 10000)
    public void publishDuePosts() {
        for (Post post : repo.findByStatusAndScheduledAtLessThanEqual(PostStatus.SCHEDULED, Instant.now())) {
            post.setStatus(PostStatus.QUEUED);
            repo.save(post);
            kafkaTemplate.send(PostTopicConfig.POSTS_TOPIC, String.valueOf(post.getId()), post.getContent());
            log.info("Post {} is due -> queued to Kafka", post.getId());
        }
    }
}