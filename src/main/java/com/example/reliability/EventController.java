package com.example.reliability;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class EventController {

    private final ProcessedEventRepository processedRepo;
    private final DeadLetterEventRepository dlqRepo;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public EventController(ProcessedEventRepository processedRepo,
                           DeadLetterEventRepository dlqRepo,
                           KafkaTemplate<String, String> kafkaTemplate) {
        this.processedRepo = processedRepo;
        this.dlqRepo = dlqRepo;
        this.kafkaTemplate = kafkaTemplate;
    }

    @GetMapping("/processed")
    public List<ProcessedEvent> processed() {
        return processedRepo.findAll();
    }

    @GetMapping("/dlq")
    public List<DeadLetterEvent> dlq() {
        return dlqRepo.findAll();
    }

    // Fix and re-send a dead-lettered event: /replay?id=103&payload=fixed
    @GetMapping("/replay")
    public String replay(@RequestParam String id, @RequestParam String payload) {
        if (dlqRepo.findFirstByEventKeyOrderByIdDesc(id).isEmpty()) {
            return "No DLQ event found with id=" + id;
        }
        kafkaTemplate.send(KafkaConfig.MAIN_TOPIC, id, payload);
        return "Replayed event id=" + id + " with payload=" + payload;
    }
}