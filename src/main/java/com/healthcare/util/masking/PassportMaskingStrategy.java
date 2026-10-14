package com.healthcare.util.masking;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.MaskingStrategy;
import org.springframework.stereotype.Component;

/**
 * Masking for international passports (alphanumeric, 6–9 chars).
 * Example: "A1234567" → "XXXXX567"
 */
@Component
public class PassportMaskingStrategy implements MaskingStrategy {

    @Override
    public DocumentType supports() {
        return DocumentType.PASSPORT;
    }

    @Override
    public String mask(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Passport value cannot be null or blank");
        }
        String val = rawValue.trim().toUpperCase();
        if (val.length() < 6 || val.length() > 9) {
            throw new IllegalArgumentException("Passport number must be 6-9 characters");
        }
        int showChars = Math.min(3, val.length());
        String masked = "X".repeat(val.length() - showChars) + val.substring(val.length() - showChars);
        return masked;
    }

    @Override
    public boolean validate(String rawValue) {
        if (rawValue == null) return false;
        return rawValue.trim().matches("[A-Z0-9]{6,9}");
    }
}
