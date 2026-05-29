package com.coderank.codeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CodeSubmissionResponse {
    private Long id;
    private String userEmail;
    private String code;
    private String language;
    private String tagName;
    private String status;
    private String output;
    private Long executionTimeMs;
    private Integer memoryUsedMb;
    private LocalDateTime submittedAt;
}
