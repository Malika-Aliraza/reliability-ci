package com.example.reliability;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.LambdaDsl;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = "reliability-service", pactVersion = PactSpecVersion.V3)
class ContractConsumerPactTest {

        @Pact(consumer = "dashboard-ui")
    RequestResponsePact processedEventsPact(PactDslWithProvider builder) {
        return builder
                .given("processed events exist")
                .uponReceiving("a request for processed events")
                    .path("/processed")
                    .method("GET")
                .willRespondWith()
                    .status(200)
                    .headers(Map.of("Content-Type", "application/json"))
                    .body(LambdaDsl.newJsonArray(array -> array.object(o -> {
                        o.stringType("eventId", "101");
                        o.stringType("payload", "hello");
                    })).build())
                .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "processedEventsPact")
    void dashboardCanReadProcessedEvents(MockServer mockServer) {
        EventClient client = new EventClient(mockServer.getUrl());

        List<Map<String, Object>> events = client.getProcessedEvents();

        assertFalse(events.isEmpty());
        assertEquals("101", events.get(0).get("eventId"));
        assertTrue(events.get(0).containsKey("payload"));
    }
}