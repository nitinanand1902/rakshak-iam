package com.healthcare.exception;

/** Thrown when a business rule is violated. Maps to HTTP 422. */
public class BusinessException extends AppException {

    public BusinessException(String message) {
        super(message, 422, "BUSINESS_RULE_VIOLATION");
    }

    public BusinessException(String message, String errorCode) {
        super(message, 422, errorCode);
    }
}
