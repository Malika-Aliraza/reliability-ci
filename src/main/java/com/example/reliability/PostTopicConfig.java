package com.example.reliability;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class PostTopicConfig {

    public static final String POSTS_TOPIC = "posts-topic";

    @Bean
    public NewTopic postsTopic() {
        return TopicBuilder.name(POSTS_TOPIC).partitions(1).replicas(1).build();
    }
}