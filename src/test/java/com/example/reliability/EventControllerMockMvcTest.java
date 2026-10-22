package com.example.reliability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class EventControllerMockMvcTest {

    @Mock ProcessedEventRepository processedRepo;
    @Mock DeadLetterEventRepository dlqRepo;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new EventController(processedRepo, dlqRepo, kafkaTemplate))
                .build();
    }

    @Test
    void processedEndpointReturnsEvents() throws Exception {
        when(processedRepo.findAll()).thenReturn(List.of(new ProcessedEvent("101", "hello")));

        mockMvc.perform(get("/processed"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventId").value("101"))
                .andExpect(jsonPath("$[0].payload").value("hello"));
    }

    @Test
    void dlqEndpointReturnsDeadLetters() throws Exception {
        when(dlqRepo.findAll()).thenReturn(List.of(new DeadLetterEvent("103", "FAIL", "boom")));

        mockMvc.perform(get("/dlq"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].eventKey").value("103"))
                .andExpect(jsonPath("$[0].reason").value("boom"));
    }

    @Test
    void replayUnknownEventIsRejected() throws Exception {
        when(dlqRepo.findFirstByEventKeyOrderByIdDesc("999")).thenReturn(Optional.empty());

        mockMvc.perform(get("/replay").param("id", "999").param("payload", "x"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No DLQ event found")));

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    void replayKnownEventResendsToMainTopic() throws Exception {
        when(dlqRepo.findFirstByEventKeyOrderByIdDesc("103"))
                .thenReturn(Optional.of(new DeadLetterEvent("103", "FAIL", "boom")));

        mockMvc.perform(get("/replay").param("id", "103").param("payload", "fixed"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Replayed")));

        verify(kafkaTemplate).send("orders-topic", "103", "fixed");
    }
}