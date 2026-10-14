package com.healthcare.exception;

/** Thrown for JWT-related issues. Maps to HTTP 401. */
public class TokenException extends AppException {

    public TokenException(String message) {
        super(message, 401, "TOKEN_ERROR");
    }

    public TokenException(String message, Throwable cause) {
        super(message, 401, "TOKEN_ERROR", cause);
    }
}
