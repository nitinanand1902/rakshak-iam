package com.healthcare.util.masking;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.MaskingStrategy;
import org.springframework.stereotype.Component;

/**
 * Masking for US Social Security Number (SSN, format: 123-45-6789).
 * Example: "123456789" or "123-45-6789" → "XXX-XX-6789"
 */
@Component
public class SsnMaskingStrategy implements MaskingStrategy {

    @Override
    public DocumentType supports() {
        return DocumentType.SSN;
    }

    @Override
    public String mask(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("SSN value cannot be null or blank");
        }
        String digits = rawValue.replaceAll("[^0-9]", "");
        if (digits.length() != 9) {
            throw new IllegalArgumentException("SSN must be 9 digits, got: " + digits.length());
        }
        return "XXX-XX-" + digits.substring(5);
    }

    @Override
    public boolean validate(String rawValue) {
        if (rawValue == null) return false;
        String digits = rawValue.replaceAll("[^0-9]", "");
        return digits.matches("\\d{9}");
    }
}
