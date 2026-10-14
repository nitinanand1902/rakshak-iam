package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.dto.response.PatientResponse;
import com.healthcare.entity.Patient;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Patient profile service.
 * <p>
 * Data-level rule: patients may only view their own profile.
 * Doctors and technicians may view any patient profile.
 */
@Service
@Slf4j
public class PatientService {

    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public PatientService(PatientRepository patientRepository, AuditService auditService) {
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    /**
     * Retrieves a patient by their DB id.
     * Callers are responsible for access-control before calling this method.
     */
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id, String requestingUsername, String ipAddress) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient", id));

        auditService.logAsync(AuditEventType.DATA_ACCESS, requestingUsername,
                "Patient", String.valueOf(id), "Patient profile accessed", ipAddress, true);

        return toResponse(patient);
    }

    /**
     * Returns the patient profile for the authenticated PATIENT user (own profile only).
     */
    @Transactional(readOnly = true)
    public PatientResponse getMyProfile(String username, String ipAddress) {
        Patient patient = patientRepository.findByUserUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Patient profile not found for user: " + username));

        auditService.logAsync(AuditEventType.DATA_ACCESS, username,
                "Patient", String.valueOf(patient.getId()), "Own profile viewed", ipAddress, true);

        return toResponse(patient);
    }

    /**
     * Paginated list of all patients — restricted to doctors/technicians via @PreAuthorize.
     */
    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable, String requestingUsername, String ipAddress) {
        auditService.logAsync(AuditEventType.DATA_ACCESS, requestingUsername,
                "Patient", "all", "Patient list accessed", ipAddress, true);
        return patientRepository.findAll(pageable).map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    private PatientResponse toResponse(Patient p) {
        return new PatientResponse(
                p.getId(),
                p.getUser().getId(),
                p.getUser().getUsername(),
                p.getFullName(),
                p.getBloodGroup(),
                p.getCountryCode(),
                p.getCreatedAt()
        );
    }
}
