package com.coderank.codeservice.service;

import com.coderank.codeservice.dto.ExecutionRequest;
import com.coderank.codeservice.dto.ExecutionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class ExecutionClient {

    private final ObjectMapper objectMapper;

    @Value("${execution.mode:sam-local}")
    private String executionMode;

    @Value("${execution.sam.endpoint:http://127.0.0.1:3001}")
    private String samEndpoint;

    @Value("${execution.aws.region:ap-south-1}")
    private String awsRegion;

    @Value("${execution.aws.profile:}")
    private String awsProfile;

    @Value("${execution.lambda.javascript:coderank-exec-js}")
    private String lambdaJavascript;

    @Value("${execution.lambda.python:coderank-exec-python}")
    private String lambdaPython;

    @Value("${execution.lambda.java:coderank-exec-java}")
    private String lambdaJava;

    @Value("${execution.default-timeout-ms:5000}")
    private Integer defaultTimeoutMs;

    private volatile LambdaClient lambdaClient;

    public ExecutionResponse execute(ExecutionRequest request) {
        ExecutionRequest payload = request;
        if (payload.getTimeoutMs() == null) {
            payload = ExecutionRequest.builder()
                    .code(request.getCode())
                    .language(request.getLanguage())
                    .tagName(request.getTagName())
                    .timeoutMs(defaultTimeoutMs)
                    .build();
        }

        String functionName = resolveLambdaName(payload.getLanguage());
        String requestJson = toJson(payload);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(functionName)
                .payload(SdkBytes.fromString(requestJson, StandardCharsets.UTF_8))
                .build();

        InvokeResponse response = getLambdaClient().invoke(invokeRequest);
        ExecutionResponse executionResponse = fromInvokeResponse(response);
        if (executionResponse == null) {
            throw new IllegalStateException("Execution lambda returned an invalid response");
        }
        return executionResponse;
    }

    private ExecutionResponse fromInvokeResponse(InvokeResponse response) {
        String payload = response.payload() == null ? "" : response.payload().asUtf8String();
        ExecutionResponse parsed = null;
        if (!payload.isBlank()) {
            try {
                parsed = objectMapper.readValue(payload, ExecutionResponse.class);
            } catch (JsonProcessingException e) {
                parsed = null;
            }
        }

        if (parsed == null) {
            parsed = ExecutionResponse.builder()
                    .status("FAILED")
                    .output(payload.isBlank() ? "Lambda returned an empty response" : payload)
                    .build();
        }

        if (response.functionError() != null && (parsed.getStatus() == null || "COMPLETED".equals(parsed.getStatus()))) {
            parsed.setStatus("FAILED");
            if (parsed.getOutput() == null || parsed.getOutput().isBlank()) {
                parsed.setOutput("Lambda error: " + response.functionError());
            }
        }

        return parsed;
    }

    private String resolveLambdaName(String language) {
        if (language == null) {
            throw new IllegalArgumentException("Language is required for execution");
        }
        String normalized = language.trim().toLowerCase();
        return switch (normalized) {
            case "javascript", "js", "node" -> lambdaJavascript;
            case "python", "py" -> lambdaPython;
            case "java" -> lambdaJava;
            default -> throw new IllegalArgumentException("Unsupported language: " + language);
        };
    }

    private String toJson(ExecutionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize execution request", e);
        }
    }

    private LambdaClient getLambdaClient() {
        LambdaClient existing = lambdaClient;
        if (existing != null) {
            return existing;
        }
        synchronized (this) {
            if (lambdaClient == null) {
                lambdaClient = buildLambdaClient();
            }
            return lambdaClient;
        }
    }

    private LambdaClient buildLambdaClient() {
        String mode = executionMode == null ? "sam-local" : executionMode.trim().toLowerCase();
        Region region = Region.of(awsRegion == null || awsRegion.isBlank() ? "ap-south-1" : awsRegion);

        if ("sam-local".equals(mode)) {
            return LambdaClient.builder()
                    .region(region)
                    .endpointOverride(URI.create(samEndpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("dummy", "dummy")))
                    .build();
        }

        if (awsProfile != null && !awsProfile.isBlank()) {
            return LambdaClient.builder()
                    .region(region)
                    .credentialsProvider(ProfileCredentialsProvider.builder()
                            .profileName(awsProfile)
                            .build())
                    .build();
        }

        return LambdaClient.builder()
                .region(region)
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}
