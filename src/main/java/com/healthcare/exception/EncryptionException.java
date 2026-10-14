package com.healthcare.exception;

/** Thrown when AES encryption or decryption fails. Maps to HTTP 500. */
public class EncryptionException extends AppException {

    public EncryptionException(String message) {
        super(message, 500, "ENCRYPTION_ERROR");
    }

    public EncryptionException(String message, Throwable cause) {
        super(message, 500, "ENCRYPTION_ERROR", cause);
    }
}
