package com.coderank.codeservice.service;

import com.coderank.codeservice.dto.CodeSaveRequest;
import com.coderank.codeservice.dto.CodeSubmitRequest;
import com.coderank.codeservice.dto.CodeSubmissionResponse;
import com.coderank.codeservice.dto.ExecutionRequest;
import com.coderank.codeservice.dto.ExecutionResponse;
import com.coderank.codeservice.model.CodeSubmission;
import com.coderank.codeservice.repository.CodeSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CodeSubmissionService {

    private final CodeSubmissionRepository repository;
    private final ExecutionClient executionClient;

    @Transactional
    public CodeSubmissionResponse submitCode(CodeSubmitRequest request, String userEmail) {
        CodeSubmission submission = CodeSubmission.builder()
                .userEmail(userEmail)
                .code(request.getCode())
                .language(request.getLanguage())
                .tagName(request.getTagName())
                .status("PENDING")
                .build();
        submission = repository.save(submission);

        try {
            ExecutionResponse executionResponse = executionClient.execute(ExecutionRequest.builder()
                    .code(request.getCode())
                    .language(request.getLanguage())
                    .tagName(request.getTagName())
                    .build());

            submission.setStatus(executionResponse.getStatus() != null ? executionResponse.getStatus() : "COMPLETED");
            submission.setOutput(executionResponse.getOutput());
            submission.setExecutionTimeMs(executionResponse.getExecutionTimeMs());
            submission.setMemoryUsedMb(executionResponse.getMemoryUsedMb());
        } catch (Exception ex) {
            submission.setStatus("FAILED");
            submission.setOutput("Execution failed: " + ex.getMessage());
        }

        submission = repository.save(submission);
        return toResponse(submission);
    }

    @Transactional
    public CodeSubmissionResponse saveCode(CodeSaveRequest request, String userEmail) {
        CodeSubmission submission = CodeSubmission.builder()
                .userEmail(userEmail)
                .code(request.getCode())
                .language(request.getLanguage())
                .tagName(request.getTagName())
                .status("SAVED")
                .build();
        submission = repository.save(submission);
        return toResponse(submission);
    }

    @Transactional(readOnly = true)
    public List<CodeSubmissionResponse> listSubmissions(String userEmail, String language, Boolean taggedOnly) {
        List<CodeSubmission> submissions;
        if (Boolean.TRUE.equals(taggedOnly)) {
            submissions = repository.findByUserEmailAndTagNameIsNotNullOrderBySubmittedAtDesc(userEmail);
        } else if (language != null && !language.isBlank()) {
            submissions = repository.findByUserEmailAndLanguageOrderBySubmittedAtDesc(userEmail, language);
        } else {
            submissions = repository.findByUserEmailOrderBySubmittedAtDesc(userEmail);
        }

        return submissions.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private CodeSubmissionResponse toResponse(CodeSubmission submission) {
        return CodeSubmissionResponse.builder()
                .id(submission.getId())
                .userEmail(submission.getUserEmail())
                .code(submission.getCode())
                .language(submission.getLanguage())
                .tagName(submission.getTagName())
                .status(submission.getStatus())
                .output(submission.getOutput())
                .executionTimeMs(submission.getExecutionTimeMs())
                .memoryUsedMb(submission.getMemoryUsedMb())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
