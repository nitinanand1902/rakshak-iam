package com.healthcare.controller;

import com.healthcare.dto.request.IdentityDocumentRequest;
import com.healthcare.dto.response.IdentityDocumentResponse;
import com.healthcare.service.IdentityDocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Identity document controller.
 * Raw document values are NEVER returned — only masked values.
 * <ul>
 *   <li>POST /{patientId}   — add document (own patient only)</li>
 *   <li>GET  /{patientId}   — list documents (own patient, or doctor/technician)</li>
 *   <li>PUT  /{id}/verify   — verify document (doctor/technician only)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/identity")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Identity Documents", description = "Country-agnostic identity document management with AES-256 encryption")
public class IdentityDocumentController {

    private final IdentityDocumentService identityDocumentService;

    public IdentityDocumentController(IdentityDocumentService identityDocumentService) {
        this.identityDocumentService = identityDocumentService;
    }

    @PostMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "Add identity document", description = "Encrypts with AES-256/GCM, stores masked value only in response.")
    public ResponseEntity<IdentityDocumentResponse> addDocument(
            @PathVariable Long patientId,
            @Valid @RequestBody IdentityDocumentRequest request,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        IdentityDocumentResponse response = identityDocumentService.addDocument(
                patientId, request, authentication.getName(), getIp(httpRequest));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/patients/{patientId}")
    @PreAuthorize("hasAnyRole('PATIENT', 'DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "List identity documents for patient", description = "Returns masked values only. Patients see their own; doctors/technicians see any.")
    public ResponseEntity<List<IdentityDocumentResponse>> getDocuments(
            @PathVariable Long patientId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        String role = authentication.getAuthorities().iterator().next().getAuthority();
        List<IdentityDocumentResponse> docs = identityDocumentService.getDocumentsForPatient(
                patientId, authentication.getName(), role, getIp(httpRequest));
        return ResponseEntity.ok(docs);
    }

    @PutMapping("/{documentId}/verify")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "Verify an identity document — doctors/technicians only")
    public ResponseEntity<IdentityDocumentResponse> verifyDocument(
            @PathVariable Long documentId,
            Authentication authentication,
            HttpServletRequest httpRequest) {
        IdentityDocumentResponse response = identityDocumentService.verifyDocument(
                documentId, authentication.getName(), getIp(httpRequest));
        return ResponseEntity.ok(response);
    }

    private String getIp(HttpServletRequest req) {
        String fwd = req.getHeader("X-Forwarded-For");
        return (fwd != null && !fwd.isBlank()) ? fwd.split(",")[0].trim() : req.getRemoteAddr();
    }
}
