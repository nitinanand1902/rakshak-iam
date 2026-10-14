package com.healthcare.service;

import com.healthcare.audit.AuditEventType;
import com.healthcare.dto.request.DiagnosisNoteRequest;
import com.healthcare.dto.response.DiagnosisNoteResponse;
import com.healthcare.entity.DiagnosisNote;
import com.healthcare.entity.Doctor;
import com.healthcare.entity.Patient;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.DiagnosisNoteRepository;
import com.healthcare.repository.DoctorRepository;
import com.healthcare.repository.PatientRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Diagnosis note service — only doctors may create notes.
 * Access-control is enforced at controller level via @PreAuthorize("hasRole('DOCTOR')").
 */
@Service
@Slf4j
public class DiagnosisService {

    private final DiagnosisNoteRepository diagnosisNoteRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AuditService auditService;

    public DiagnosisService(DiagnosisNoteRepository diagnosisNoteRepository,
                            DoctorRepository doctorRepository,
                            PatientRepository patientRepository,
                            AuditService auditService) {
        this.diagnosisNoteRepository = diagnosisNoteRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.auditService = auditService;
    }

    /**
     * Creates a diagnosis note. The doctor username is derived from the authenticated principal.
     */
    @Transactional
    public DiagnosisNoteResponse createNote(DiagnosisNoteRequest request,
                                            String doctorUsername, String ipAddress) {
        Doctor doctor = doctorRepository.findByUserUsername(doctorUsername)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor profile not found for: " + doctorUsername));

        Patient patient = patientRepository.findById(request.patientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient", request.patientId()));

        DiagnosisNote note = DiagnosisNote.builder()
                .doctor(doctor)
                .patient(patient)
                .diagnosisText(request.diagnosisText())
                .icdCode(request.icdCode())
                .treatmentPlan(request.treatmentPlan())
                .build();

        note = diagnosisNoteRepository.save(note);

        auditService.log(AuditEventType.DIAGNOSIS_NOTE_CREATED, doctorUsername,
                "DiagnosisNote", String.valueOf(note.getId()),
                "Diagnosis created for patient " + request.patientId(), ipAddress, true);

        log.info("Diagnosis note created by {} for patient {}", doctorUsername, request.patientId());
        return toResponse(note);
    }

    /** Returns all diagnosis notes for a patient, paginated. */
    @Transactional(readOnly = true)
    public Page<DiagnosisNoteResponse> getNotesForPatient(Long patientId, String requestingUsername,
                                                           String ipAddress, Pageable pageable) {
        if (!patientRepository.existsById(patientId)) {
            throw new ResourceNotFoundException("Patient", patientId);
        }
        auditService.logAsync(AuditEventType.DATA_ACCESS, requestingUsername,
                "DiagnosisNote", "patient:" + patientId, "Diagnosis notes accessed", ipAddress, true);

        return diagnosisNoteRepository.findByPatientId(patientId, pageable).map(this::toResponse);
    }

    // -------------------------------------------------------------------------
    private DiagnosisNoteResponse toResponse(DiagnosisNote note) {
        return new DiagnosisNoteResponse(
                note.getId(),
                note.getPatient().getId(),
                note.getPatient().getFullName(),
                note.getDoctor().getId(),
                note.getDoctor().getFullName(),
                note.getDiagnosisText(),
                note.getIcdCode(),
                note.getTreatmentPlan(),
                note.getCreatedAt()
        );
    }
}
