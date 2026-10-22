package com.example.reliability;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

@Configuration
public class KafkaConfig {

    public static final String MAIN_TOPIC = "orders-topic";
    public static final String DLQ_TOPIC  = "orders-dlq";

    @Bean
    public NewTopic ordersTopic() {
        return TopicBuilder.name(MAIN_TOPIC).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic ordersDlq() {
        return TopicBuilder.name(DLQ_TOPIC).partitions(1).replicas(1).build();
    }

    // Retry with exponential backoff, then publish to DLQ
    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<String, String> template) {

        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, ex) -> new TopicPartition(DLQ_TOPIC, -1));

        ExponentialBackOff backOff = new ExponentialBackOff(1000L, 2.0); // 1s, 2s, 4s...
        backOff.setMaxElapsedTime(7000L);  // stop after ~3 retries
        backOff.setMaxInterval(10000L);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}