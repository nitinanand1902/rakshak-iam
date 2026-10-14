package com.healthcare.util.masking;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.MaskingStrategy;
import org.springframework.stereotype.Component;

/**
 * Generic masking for NATIONAL_ID and DRIVING_LICENSE.
 * Shows only the last 4 characters, masks the rest with 'X'.
 */
@Component
public class GenericDocumentMaskingStrategy implements MaskingStrategy {

    @Override
    public DocumentType supports() {
        // This is used as a fallback; the registry also maps DRIVING_LICENSE to this
        return DocumentType.NATIONAL_ID;
    }

    @Override
    public String mask(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Document value cannot be null or blank");
        }
        String val = rawValue.trim();
        if (val.length() <= 4) return val;
        return "X".repeat(val.length() - 4) + val.substring(val.length() - 4);
    }

    @Override
    public boolean validate(String rawValue) {
        return rawValue != null && rawValue.trim().length() >= 4;
    }
}
