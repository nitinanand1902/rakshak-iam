package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Diagnostic report uploaded by a Technician. */
@Entity
@Table(
    name = "diagnostic_reports",
    indexes = {
        @Index(name = "idx_report_patient", columnList = "patient_id"),
        @Index(name = "idx_report_technician", columnList = "technician_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosticReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long patientId;

    @Column(nullable = false)
    private Long technicianId;

    @Column(nullable = false, length = 100)
    private String reportType;

    @Column(length = 2000)
    private String reportContent;

    /** In production this would be an S3 URL or file path. */
    @Column(length = 500)
    private String fileReference;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
