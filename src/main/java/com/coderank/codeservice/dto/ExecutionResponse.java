package com.coderank.codeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResponse {
    private String status;
    private String output;
    private Long executionTimeMs;
    private Integer memoryUsedMb;
}
