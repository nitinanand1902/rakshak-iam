package com.healthcare.dto.response;

import com.healthcare.entity.DocumentType;
import com.healthcare.entity.VerificationStatus;
import java.time.LocalDateTime;

/**
 * Identity document response.
 * Only the masked value is returned — encrypted or raw values are NEVER exposed.
 */
public record IdentityDocumentResponse(
        Long id,
        DocumentType documentType,
        String maskedValue,
        String countryCode,
        VerificationStatus verificationStatus,
        LocalDateTime createdAt
) {}
