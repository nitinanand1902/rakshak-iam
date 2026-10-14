package com.healthcare.dto.response;

import java.time.LocalDateTime;

/** Diagnosis note response. */
public record DiagnosisNoteResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String diagnosisText,
        String icdCode,
        String treatmentPlan,
        LocalDateTime createdAt
) {}
