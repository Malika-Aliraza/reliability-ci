package com.example.reliability;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.KafkaHeaders;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderConsumerTest {

    @Mock ProcessedEventRepository processedRepo;
    @Mock DeadLetterEventRepository dlqRepo;

    OrderConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrderConsumer(processedRepo, dlqRepo);
    }

    private ConsumerRecord<String, String> rec(String key, String value) {
        return new ConsumerRecord<>("orders-topic", 0, 0L, key, value);
    }

    @Test
    void savesEventOnSuccess() {
        consumer.consume(rec("1", "hello"));
        verify(processedRepo).save(any(ProcessedEvent.class));
    }

    @Test
    void ignoresDuplicateEvent() {
        when(processedRepo.existsById("1")).thenReturn(true);
        consumer.consume(rec("1", "hello"));
        verify(processedRepo, never()).save(any());
    }

    @Test
    void permanentFailureThrowsAndIsNotSaved() {
        assertThrows(RuntimeException.class, () -> consumer.consume(rec("3", "FAIL")));
        verify(processedRepo, never()).save(any());
    }

    @Test
    void flakyEventSucceedsOnThirdAttempt() {
        assertThrows(RuntimeException.class, () -> consumer.consume(rec("2", "FLAKY")));
        assertThrows(RuntimeException.class, () -> consumer.consume(rec("2", "FLAKY")));
        consumer.consume(rec("2", "FLAKY"));
        verify(processedRepo, times(1)).save(any(ProcessedEvent.class));
    }

    @Test
    void dlqListenerStoresFailureReason() {
        ConsumerRecord<String, String> r = rec("3", "FAIL");
        r.headers().add(KafkaHeaders.DLT_EXCEPTION_MESSAGE, "boom".getBytes(StandardCharsets.UTF_8));
        consumer.consumeDlq(r);
        verify(dlqRepo).save(argThat(e -> "boom".equals(e.getReason()) && "3".equals(e.getEventKey())));
    }
}