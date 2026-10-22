package com.example.reliability;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProducerController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ProducerController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    // e.g. http://localhost:8080/send?id=1&payload=hello
    @GetMapping("/send")
    public String send(@RequestParam String id, @RequestParam String payload) {
        kafkaTemplate.send(KafkaConfig.MAIN_TOPIC, id, payload);
        return "Sent event id=" + id + " payload=" + payload;
    }
}