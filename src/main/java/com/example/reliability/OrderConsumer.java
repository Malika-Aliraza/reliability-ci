package com.example.reliability;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderConsumer.class);

    private final ProcessedEventRepository processedRepo;
    private final DeadLetterEventRepository dlqRepo;
    private final Map<String, AtomicInteger> attempts = new ConcurrentHashMap<>();

    public OrderConsumer(ProcessedEventRepository processedRepo,
                         DeadLetterEventRepository dlqRepo) {
        this.processedRepo = processedRepo;
        this.dlqRepo = dlqRepo;
    }

    @KafkaListener(topics = KafkaConfig.MAIN_TOPIC, groupId = "order-group")
    public void consume(ConsumerRecord<String, String> record) {
        String eventId = record.key();
        String payload = record.value();

        // 1. Idempotency check against the database
        if (processedRepo.existsById(eventId)) {
            log.warn("DUPLICATE event {} ignored (found in DB)", eventId);
            return;
        }

        log.info("Processing event {} payload={}", eventId, payload);

        // 2. Simulated failures
        if (payload.contains("FAIL")) {
            log.error("Permanent failure for event {}", eventId);
            throw new RuntimeException("Permanent failure for event " + eventId);
        }
        if (payload.contains("FLAKY")) {
            int n = attempts.computeIfAbsent(eventId, k -> new AtomicInteger()).incrementAndGet();
            if (n < 3) {
                log.error("Temporary failure for event {} (attempt {})", eventId, n);
                throw new RuntimeException("Temporary failure, attempt " + n);
            }
        }

        // 3. Success -> persist event ID
        processedRepo.save(new ProcessedEvent(eventId, payload));
        log.info("SUCCESS: event {} processed and saved to DB", eventId);
    }

    @KafkaListener(topics = KafkaConfig.DLQ_TOPIC, groupId = "dlq-group")
    public void consumeDlq(ConsumerRecord<String, String> record) {
        Header h = record.headers().lastHeader(KafkaHeaders.DLT_EXCEPTION_MESSAGE);
        String reason = (h != null) ? new String(h.value(), StandardCharsets.UTF_8) : "unknown";
        dlqRepo.save(new DeadLetterEvent(record.key(), record.value(), reason));
        log.error("DLQ RECEIVED -> key={} value={} reason={}", record.key(), record.value(), reason);
    }
}