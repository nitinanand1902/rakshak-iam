package com.healthcare.util.masking;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.MaskingStrategy;
import org.springframework.stereotype.Component;

/**
 * Masking for Indian PAN card (format: ABCDE1234F).
 * Example: "ABCDE1234F" → "XXXXX1234F"
 */
@Component
public class PanMaskingStrategy implements MaskingStrategy {

    @Override
    public DocumentType supports() {
        return DocumentType.PAN;
    }

    @Override
    public String mask(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("PAN value cannot be null or blank");
        }
        String val = rawValue.trim().toUpperCase();
        if (!validate(val)) {
            throw new IllegalArgumentException("Invalid PAN format: must be AAAAA9999A");
        }
        return "XXXXX" + val.substring(5);
    }

    @Override
    public boolean validate(String rawValue) {
        if (rawValue == null) return false;
        return rawValue.trim().toUpperCase().matches("[A-Z]{5}[0-9]{4}[A-Z]");
    }
}
