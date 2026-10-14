package com.healthcare.entity;

/**
 * Supported identity document types.
 * <p>
 * Extensibility: Add a new enum value to support a new document type.
 * Pair it with a new {@link com.healthcare.util.MaskingStrategy} implementation
 * in the masking package. No existing code needs modification (OCP).
 */
public enum DocumentType {
    /** India — 12-digit Aadhaar number */
    AADHAAR,
    /** International passport */
    PASSPORT,
    /** India — Permanent Account Number */
    PAN,
    /** USA — Social Security Number */
    SSN,
    /** Generic national identity card */
    NATIONAL_ID,
    /** Driver's license */
    DRIVING_LICENSE
}
