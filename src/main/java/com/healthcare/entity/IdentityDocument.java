package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Country-agnostic identity document.
 * <p>
 * Raw values are NEVER stored. Only AES-256/GCM encrypted ciphertext and
 * a pre-computed masked display value are persisted.
 * <p>
 * Extensibility: Adding support for a new country's document type requires:
 * 1. Adding a constant to {@link DocumentType}
 * 2. Creating a {@link com.healthcare.util.MaskingStrategy} implementation
 * No changes to this entity or any service.
 */
@Entity
@Table(
    name = "identity_documents",
    indexes = {
        @Index(name = "idx_identity_patient", columnList = "patient_id"),
        @Index(name = "idx_identity_type_country", columnList = "document_type,country_code")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IdentityDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentType documentType;

    /** AES-256/GCM encrypted document value. Format: base64(iv + ciphertext). Never expose raw. */
    @Column(nullable = false, length = 512)
    private String encryptedDocumentValue;

    /**
     * Pre-computed masked display value (e.g., XXXXXXXX9012).
     * Safe to include in API responses.
     */
    @Column(nullable = false, length = 50)
    private String maskedValue;

    /** ISO 3166-1 alpha-2 country code (e.g., IN, US, CN, GB). */
    @Column(nullable = false, length = 5)
    private String countryCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VerificationStatus verificationStatus = VerificationStatus.PENDING;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime verifiedAt;
}
