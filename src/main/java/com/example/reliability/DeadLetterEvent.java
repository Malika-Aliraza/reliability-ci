package com.example.reliability;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "dead_letter_events")
public class DeadLetterEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String eventKey;
    private String payload;
    @Column(length = 1000)
    private String reason;
    private Instant receivedAt = Instant.now();

    public DeadLetterEvent() {}
    public DeadLetterEvent(String eventKey, String payload, String reason) {
        this.eventKey = eventKey;
        this.payload = payload;
        this.reason = reason;
    }
    public Long getId() { return id; }
    public String getEventKey() { return eventKey; }
    public String getPayload() { return payload; }
    public String getReason() { return reason; }
    public Instant getReceivedAt() { return receivedAt; }
}