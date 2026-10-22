package com.example.reliability;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "processed_events")
public class ProcessedEvent {
    @Id
    private String eventId;          // primary key = uniqueness guarantee
    private String payload;
    private Instant processedAt = Instant.now();

    public ProcessedEvent() {}
    public ProcessedEvent(String eventId, String payload) {
        this.eventId = eventId;
        this.payload = payload;
    }
    public String getEventId() { return eventId; }
    public String getPayload() { return payload; }
    public Instant getProcessedAt() { return processedAt; }
}