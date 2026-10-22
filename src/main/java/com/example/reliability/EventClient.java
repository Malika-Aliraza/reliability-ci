package com.example.reliability;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

public class EventClient {

    private final RestTemplate rest = new RestTemplate();
    private final String baseUrl;

    public EventClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<Map<String, Object>> getProcessedEvents() {
        return rest.exchange(baseUrl + "/processed", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<Map<String, Object>>>() {}).getBody();
    }
}