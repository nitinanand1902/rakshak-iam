package com.healthcare.service;

import com.healthcare.dto.response.IamEvaluationResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.IdentityDocumentRepository;
import com.healthcare.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * IAM Evaluation Service — computes the security posture of a user account.
 * <p>
 * Evaluates:
 * - Account status (active, locked, disabled)
 * - Identity document verification completeness
 * - Failed login attempt history
 * - Overall risk rating: LOW / MEDIUM / HIGH / CRITICAL
 * <p>
 * Extensibility: New checks can be added without touching existing logic.
 */
@Service
@Slf4j
public class IamEvaluationService {

    private final UserRepository userRepository;
    private final IdentityDocumentRepository identityDocumentRepository;

    public IamEvaluationService(UserRepository userRepository,
                                IdentityDocumentRepository identityDocumentRepository) {
        this.userRepository = userRepository;
        this.identityDocumentRepository = identityDocumentRepository;
    }

    /**
     * Evaluates the IAM posture for the given username.
     *
     * @param username the user to evaluate
     * @return a detailed evaluation report
     */
    @Transactional(readOnly = true)
    public IamEvaluationResponse evaluate(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));

        List<IdentityDocument> docs = identityDocumentRepository.findByPatientUserId(user.getId());
        boolean hasVerified = docs.stream()
                .anyMatch(d -> d.getVerificationStatus() == VerificationStatus.VERIFIED);

        List<String> flags = new ArrayList<>();
        computeFlags(user, docs, hasVerified, flags);

        String risk = computeRisk(user, docs, hasVerified, flags);

        List<IamEvaluationResponse.DocumentSummary> docSummaries = docs.stream()
                .map(d -> new IamEvaluationResponse.DocumentSummary(
                        d.getDocumentType(), d.getCountryCode(),
                        d.getVerificationStatus(), d.getMaskedValue()))
                .toList();

        log.info("IAM evaluation completed for user={}, risk={}", username, risk);

        return new IamEvaluationResponse(
                user.getUsername(),
                user.getRole().name(),
                user.isEnabled(),
                user.isAccountLocked(),
                user.getFailedLoginAttempts(),
                hasVerified,
                docs.size(),
                docSummaries,
                flags,
                risk,
                LocalDateTime.now()
        );
    }

    // -------------------------------------------------------------------------
    // Private evaluation logic — each check is isolated for easy extension
    // -------------------------------------------------------------------------

    private void computeFlags(User user, List<IdentityDocument> docs,
                              boolean hasVerified, List<String> flags) {
        if (!user.isEnabled()) {
            flags.add("ACCOUNT_DISABLED");
        }
        if (user.isAccountLocked()) {
            flags.add("ACCOUNT_LOCKED");
        }
        if (user.getFailedLoginAttempts() > 0) {
            flags.add("FAILED_LOGIN_ATTEMPTS: " + user.getFailedLoginAttempts());
        }
        if (docs.isEmpty()) {
            flags.add("NO_IDENTITY_DOCUMENT");
        } else if (!hasVerified) {
            flags.add("IDENTITY_NOT_VERIFIED");
        }
        long pendingDocs = docs.stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.PENDING).count();
        if (pendingDocs > 0) {
            flags.add("PENDING_DOCUMENTS: " + pendingDocs);
        }
        long rejectedDocs = docs.stream()
                .filter(d -> d.getVerificationStatus() == VerificationStatus.REJECTED).count();
        if (rejectedDocs > 0) {
            flags.add("REJECTED_DOCUMENTS: " + rejectedDocs);
        }
    }

    private String computeRisk(User user, List<IdentityDocument> docs,
                               boolean hasVerified, List<String> flags) {
        // CRITICAL: account disabled or locked with high failure count
        if (!user.isEnabled() || (user.isAccountLocked() && user.getFailedLoginAttempts() >= 5)) {
            return "CRITICAL";
        }
        // HIGH: no identity document or all documents rejected
        if (docs.isEmpty() || docs.stream().allMatch(
                d -> d.getVerificationStatus() == VerificationStatus.REJECTED)) {
            return "HIGH";
        }
        // MEDIUM: identity pending or some failed attempts
        if (!hasVerified || user.getFailedLoginAttempts() >= 3) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
