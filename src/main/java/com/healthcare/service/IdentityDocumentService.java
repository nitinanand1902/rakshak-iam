package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.dto.request.IdentityDocumentRequest;
import com.healthcare.dto.response.IdentityDocumentResponse;
import com.healthcare.entity.*;
import com.healthcare.exception.BusinessException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.IdentityDocumentRepository;
import com.healthcare.repository.PatientRepository;
import com.healthcare.util.EncryptionService;
import com.healthcare.util.MaskingStrategy;
import com.healthcare.util.MaskingStrategyRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Identity document management service.
 * <p>
 * - Raw values encrypted with AES-256/GCM before DB persist
 * - Masked values computed via strategy registry (country/doc-type agnostic)
 * - Data-level access control: patients can only access their own documents
 */
@Service
@Slf4j
public class IdentityDocumentService {

    private final IdentityDocumentRepository identityDocumentRepository;
    private final PatientRepository patientRepository;
    private final EncryptionService encryptionService;
    private final MaskingStrategyRegistry maskingStrategyRegistry;
    private final AuditService auditService;

    public IdentityDocumentService(IdentityDocumentRepository identityDocumentRepository,
                                   PatientRepository patientRepository,
                                   EncryptionService encryptionService,
                                   MaskingStrategyRegistry maskingStrategyRegistry,
                                   AuditService auditService) {
        this.identityDocumentRepository = identityDocumentRepository;
        this.patientRepository = patientRepository;
        this.encryptionService = encryptionService;
        this.maskingStrategyRegistry = maskingStrategyRegistry;
        this.auditService = auditService;
    }

    /**
     * Adds an identity document for the given patient.
     * Raw value is encrypted; only masked value is returned.
     *
     * @param patientId    the patient's DB id
     * @param request      document details
     * @param requestingUsername authenticated username (for data-level check)
     * @param ipAddress    caller IP
     * @return masked document response
     */
    @Transactional
    public IdentityDocumentResponse addDocument(Long patientId, IdentityDocumentRequest request,
                                                String requestingUsername, String ipAddress) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Data-level access control: only own patient can add their document
        enforcePatientOwnership(patient, requestingUsername);

        DocumentType docType;
        try {
            docType = DocumentType.valueOf(request.documentType().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid document type: '" + request.documentType() +
                    "'. Supported: AADHAAR, SSN, PASSPORT, PAN, NATIONAL_ID, DRIVING_LICENSE");
        }

        MaskingStrategy strategy = maskingStrategyRegistry.resolve(docType);

        if (!strategy.validate(request.documentValue())) {
            throw new BusinessException("Invalid format for document type: " + docType);
        }

        String masked = strategy.mask(request.documentValue());
        String encrypted = encryptionService.encrypt(request.documentValue());

        IdentityDocument doc = IdentityDocument.builder()
                .patient(patient)
                .documentType(docType)
                .encryptedDocumentValue(encrypted)
                .maskedValue(masked)
                .countryCode(request.countryCode().toUpperCase())
                .verificationStatus(VerificationStatus.PENDING)
                .build();

        doc = identityDocumentRepository.save(doc);

        auditService.log(AuditEventType.IDENTITY_DOCUMENT_ADDED, requestingUsername,
                "IdentityDocument", String.valueOf(doc.getId()),
                "Added " + docType + " document for patient " + patientId, ipAddress, true);

        log.info("Identity document added: type={}, patient={}", docType, patientId);
        return toResponse(doc);
    }

    /**
     * Returns all identity documents for a patient (masked values only).
     *
     * @param patientId            the patient's DB id
     * @param requestingUsername   caller username
     * @param requestingRole       caller role string (ROLE_DOCTOR etc.) — doctors can view all
     * @param ipAddress            caller IP
     */
    @Transactional(readOnly = true)
    public List<IdentityDocumentResponse> getDocumentsForPatient(Long patientId,
                                                                  String requestingUsername,
                                                                  String requestingRole,
                                                                  String ipAddress) {
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", patientId));

        // Patients can only view their own; doctors/technicians can view any
        if (requestingRole.contains("PATIENT")) {
            enforcePatientOwnership(patient, requestingUsername);
        }

        auditService.logAsync(AuditEventType.IDENTITY_DOCUMENT_VIEWED, requestingUsername,
                "IdentityDocument", "patient:" + patientId,
                "Viewed identity documents for patient " + patientId, ipAddress, true);

        return identityDocumentRepository.findByPatientId(patientId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Marks a document as verified. Only a DOCTOR or TECHNICIAN should call this
     * (enforced at controller level via @PreAuthorize).
     */
    @Transactional
    public IdentityDocumentResponse verifyDocument(Long documentId, String verifierUsername, String ipAddress) {
        IdentityDocument doc = identityDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResourceNotFoundException("IdentityDocument", documentId));

        if (doc.getVerificationStatus() == VerificationStatus.VERIFIED) {
            throw new BusinessException("Document " + documentId + " is already verified");
        }

        doc.setVerificationStatus(VerificationStatus.VERIFIED);
        doc.setVerifiedAt(java.time.LocalDateTime.now());
        doc = identityDocumentRepository.save(doc);

        auditService.log(AuditEventType.IDENTITY_DOCUMENT_VERIFIED, verifierUsername,
                "IdentityDocument", String.valueOf(documentId),
                "Document verified: " + doc.getDocumentType(), ipAddress, true);

        log.info("Document {} verified by {}", documentId, verifierUsername);
        return toResponse(doc);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void enforcePatientOwnership(Patient patient, String requestingUsername) {
        if (!patient.getUser().getUsername().equals(requestingUsername)) {
            throw new AccessDeniedException("You can only access your own identity documents");
        }
    }

    private IdentityDocumentResponse toResponse(IdentityDocument doc) {
        return new IdentityDocumentResponse(
                doc.getId(),
                doc.getDocumentType(),
                doc.getMaskedValue(),
                doc.getCountryCode(),
                doc.getVerificationStatus(),
                doc.getCreatedAt()
        );
    }
}
