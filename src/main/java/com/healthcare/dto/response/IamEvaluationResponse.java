package com.healthcare.dto.response;

import com.healthcare.entity.DocumentType;
import com.healthcare.entity.VerificationStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * IAM evaluation result — summarises security posture for a user.
 * Returned by the IAM evaluation endpoint.
 */
public record IamEvaluationResponse(
        String username,
        String role,
        boolean accountActive,
        boolean accountLocked,
        int failedLoginAttempts,
        boolean hasVerifiedIdentity,
        int totalIdentityDocuments,
        List<DocumentSummary> identityDocuments,
        List<String> securityFlags,
        String overallRisk,
        LocalDateTime evaluatedAt
) {
    public record DocumentSummary(
            DocumentType documentType,
            String countryCode,
            VerificationStatus status,
            String maskedValue
    ) {}
}
