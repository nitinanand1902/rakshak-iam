package com.healthcare.util;

import com.healthcare.entity.DocumentType;

/**
 * Strategy interface for masking identity document values before storage or display.
 * <p>
 * Open/Closed Principle: To support a new document type, implement this interface
 * and register it — no existing code changes needed.
 * <p>
 * Example: AADHAAR "123412341234" → "XXXX-XXXX-1234"
 */
public interface MaskingStrategy {

    /**
     * Returns the {@link DocumentType} this strategy handles.
     *
     * @return the supported document type
     */
    DocumentType supports();

    /**
     * Masks the raw document value for safe display/storage.
     *
     * @param rawValue the plain-text document number/value
     * @return masked representation safe to store and show in API responses
     * @throws IllegalArgumentException if rawValue is null or invalid for this document type
     */
    String mask(String rawValue);

    /**
     * Validates the format of the raw document value.
     *
     * @param rawValue the plain-text document number/value
     * @return true if valid, false otherwise
     */
    boolean validate(String rawValue);
}
