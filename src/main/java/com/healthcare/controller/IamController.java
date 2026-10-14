package com.healthcare.controller;

import com.healthcare.dto.response.IamEvaluationResponse;
import com.healthcare.service.IamEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * IAM evaluation controller.
 * - Any authenticated user can evaluate their own posture.
 * - Doctors and technicians can evaluate any user's posture.
 */
@RestController
@RequestMapping("/api/v1/iam")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "IAM Evaluation", description = "Security posture evaluation — risk rating, identity check, account status")
public class IamController {

    private final IamEvaluationService iamEvaluationService;

    public IamController(IamEvaluationService iamEvaluationService) {
        this.iamEvaluationService = iamEvaluationService;
    }

    @GetMapping("/evaluate/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Evaluate own IAM posture", description = "Returns security flags, risk level, and identity document status for the calling user.")
    public ResponseEntity<IamEvaluationResponse> evaluateMyPosture(Authentication authentication) {
        return ResponseEntity.ok(iamEvaluationService.evaluate(authentication.getName()));
    }

    @GetMapping("/evaluate/{username}")
    @PreAuthorize("hasAnyRole('DOCTOR', 'TECHNICIAN')")
    @Operation(summary = "Evaluate IAM posture for any user — doctors/technicians only")
    public ResponseEntity<IamEvaluationResponse> evaluateUserPosture(@PathVariable String username) {
        return ResponseEntity.ok(iamEvaluationService.evaluate(username));
    }
}
