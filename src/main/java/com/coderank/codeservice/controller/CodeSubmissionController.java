package com.coderank.codeservice.controller;

import com.coderank.codeservice.dto.CodeSaveRequest;
import com.coderank.codeservice.dto.CodeSubmitRequest;
import com.coderank.codeservice.dto.CodeSubmissionResponse;
import com.coderank.codeservice.service.CodeSubmissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/code")
@RequiredArgsConstructor
public class CodeSubmissionController {

    private final CodeSubmissionService codeSubmissionService;

    @PostMapping("/submit")
    public ResponseEntity<CodeSubmissionResponse> submitCode(
            @Valid @RequestBody CodeSubmitRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(codeSubmissionService.submitCode(request, userEmail));
    }

    @PostMapping("/save")
    public ResponseEntity<CodeSubmissionResponse> saveCode(
            @Valid @RequestBody CodeSaveRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(codeSubmissionService.saveCode(request, userEmail));
    }

    @GetMapping("/submissions")
    public ResponseEntity<List<CodeSubmissionResponse>> listSubmissions(
            Authentication authentication,
            @RequestParam(required = false) String language,
            @RequestParam(required = false, defaultValue = "false") Boolean taggedOnly) {
        String userEmail = authentication.getName();
        return ResponseEntity.ok(codeSubmissionService.listSubmissions(userEmail, language, taggedOnly));
    }
}
