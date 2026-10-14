package com.healthcare.audit;

/**
 * Catalogue of all auditable events in the system.
 * <p>
 * Extensibility: Add a new constant here to track a new event.
 * No changes to {@link com.healthcare.service.AuditService} required.
 */
public enum AuditEventType {

    // Authentication
    USER_REGISTERED,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    LOGOUT,
    ACCOUNT_LOCKED,
    ACCOUNT_UNLOCKED,
    TOKEN_REFRESHED,
    TOKEN_REVOKED,

    // SSO / Federated Authentication
    SSO_LOGIN_SUCCESS,
    SSO_LOGIN_FAILED,
    SAML_LOGIN_SUCCESS,
    SAML_LOGIN_FAILED,
    SSO_PROVISIONING_STARTED,
    SSO_PROVISIONING_COMPLETED,

    // Identity Documents
    IDENTITY_DOCUMENT_ADDED,
    IDENTITY_DOCUMENT_VIEWED,
    IDENTITY_DOCUMENT_VERIFIED,
    IDENTITY_DOCUMENT_REJECTED,
    IDENTITY_DOCUMENT_DELETED,

    // Data Access
    DATA_ACCESS,
    DATA_EXPORT,

    // Clinical
    DIAGNOSIS_NOTE_CREATED,
    DIAGNOSIS_NOTE_VIEWED,
    DIAGNOSTIC_REPORT_CREATED,
    DIAGNOSTIC_REPORT_VIEWED,

    // Administration
    USER_UPDATED,
    USER_DELETED,
    PASSWORD_CHANGED,
    ROLE_CHANGED,

    // Security
    ACCESS_DENIED,
    SUSPICIOUS_ACTIVITY
}
