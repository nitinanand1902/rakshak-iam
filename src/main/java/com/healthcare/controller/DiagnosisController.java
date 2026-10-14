package com.healthcare.controller;

import com.healthcare.dto.request.DiagnosisNoteRequest;
import com.healthcare.dto.response.DiagnosisNoteResponse;
import com.healthcare.service.DiagnosisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Diagnosis controller — doctor-only create, doctor/technician/patient read.
 */
@RestController
@RequestMapping("/api/v1/diagnosis")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Diagnosis Notes", description = "Clinical diagnosis management — doctor-only write access")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(summary = "Create diagnosis note — doctors only")
    public ResponseEntity<DiagnosisNoteResponse> createNote(
            @Valid @RequestBody DiagnosisNoteRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        DiagnosisNoteResponse response = diagnosisService.createNote(
                request, authentication.getName(), getIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN', 'PATIENT')")
    @Operation(summary = "List diagnosis notes for a patient")
    public ResponseEntity<Page<DiagnosisNoteResponse>> getNotesForPatient(
            @PathVariable Long patientId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        Page<DiagnosisNoteResponse> notes = diagnosisService.getNotesForPatient(
                patientId, authentication.getName(), getIp(httpRequest), pageable);
        return ResponseEntity.ok(notes);
    }

    private String getIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
