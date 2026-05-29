package com.coderank.codeservice.service;

import com.coderank.codeservice.dto.ExecutionRequest;
import com.coderank.codeservice.dto.ExecutionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
public class ExecutionClient {

    private final RestTemplate restTemplate;

    @Value("${execution-service.url}")
    private String executionUrl;

    public ExecutionResponse execute(ExecutionRequest request) {
        ResponseEntity<ExecutionResponse> response = restTemplate.postForEntity(
                executionUrl, request, ExecutionResponse.class);
        ExecutionResponse body = response.getBody();
        if (!response.getStatusCode().is2xxSuccessful() || body == null) {
            throw new IllegalStateException("Execution service returned an invalid response");
        }
        return body;
    }
}
