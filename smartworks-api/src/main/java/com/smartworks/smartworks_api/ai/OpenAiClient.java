package com.smartworks.smartworks_api.ai;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class OpenAiClient {

    private final RestClient rest;
    private final String apiKey;

    public OpenAiClient(@Value("${OPENAI_API_KEY:}") String apiKey) {
        this.apiKey = apiKey;
        this.rest = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Map<String, Object> createResponse(Map<String, Object> body) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY no está configurada");
        }

        return rest.post()
                .uri("/responses")
                .body(body)
                .retrieve()
                .body(Map.class);
    }
}
