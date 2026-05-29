package com.coderank.codeservice.repository;

import com.coderank.codeservice.model.CodeSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CodeSubmissionRepository extends JpaRepository<CodeSubmission, Long> {
    List<CodeSubmission> findByUserEmailOrderBySubmittedAtDesc(String userEmail);
    List<CodeSubmission> findByUserEmailAndTagNameIsNotNullOrderBySubmittedAtDesc(String userEmail);
    List<CodeSubmission> findByUserEmailAndLanguageOrderBySubmittedAtDesc(String userEmail, String language);
}
