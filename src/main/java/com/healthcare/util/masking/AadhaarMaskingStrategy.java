package com.healthcare.util.masking;

import com.healthcare.entity.DocumentType;
import com.healthcare.util.MaskingStrategy;
import org.springframework.stereotype.Component;

/**
 * Masking for Indian Aadhaar card (12-digit number).
 * Example: "123412341234" → "XXXX-XXXX-1234"
 */
@Component
public class AadhaarMaskingStrategy implements MaskingStrategy {

    @Override
    public DocumentType supports() {
        return DocumentType.AADHAAR;
    }

    @Override
    public String mask(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Aadhaar value cannot be null or blank");
        }
        String digits = rawValue.replaceAll("\\s+", "");
        if (digits.length() != 12) {
            throw new IllegalArgumentException("Aadhaar must be 12 digits, got: " + digits.length());
        }
        return "XXXX-XXXX-" + digits.substring(8);
    }

    @Override
    public boolean validate(String rawValue) {
        if (rawValue == null) return false;
        String digits = rawValue.replaceAll("\\s+", "");
        return digits.matches("\\d{12}");
    }
}
