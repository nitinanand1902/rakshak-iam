package com.healthcare.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Request to create a diagnosis note. */
public record DiagnosisNoteRequest(
        @NotNull(message = "Patient ID is required")
        Long patientId,

        @NotBlank(message = "Diagnosis text is required")
        String diagnosisText,

        String icdCode,

        String treatmentPlan
) {}
