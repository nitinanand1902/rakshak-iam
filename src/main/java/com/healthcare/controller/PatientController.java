package com.healthcare.controller;

import com.healthcare.dto.response.PatientResponse;
import com.healthcare.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Patient controller — RBAC + data-level access control.
 * <ul>
 *   <li>GET /me        — any PATIENT (own profile only)</li>
 *   <li>GET /{id}      — DOCTOR or TECHNICIAN</li>
 *   <li>GET /          — DOCTOR or TECHNICIAN (paginated)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/patients")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Patients", description = "Patient profile management")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PATIENT')")
    @Operation(summary = "Get own patient profile")
    public ResponseEntity<PatientResponse> getMyProfile(
            Authentication authentication, HttpServletRequest request) {
        return ResponseEntity.ok(
                patientService.getMyProfile(authentication.getName(), getIp(request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "Get patient by ID — doctors/technicians only")
    public ResponseEntity<PatientResponse> getPatientById(
            @PathVariable Long id,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                patientService.getPatientById(id, authentication.getName(), getIp(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "List all patients — doctors/technicians only, paginated")
    public ResponseEntity<Page<PatientResponse>> getAllPatients(
            @PageableDefault(size = 20, sort = "id") Pageable pageable,
            Authentication authentication,
            HttpServletRequest request) {
        return ResponseEntity.ok(
                patientService.getAllPatients(pageable, authentication.getName(), getIp(request)));
    }

    private String getIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
