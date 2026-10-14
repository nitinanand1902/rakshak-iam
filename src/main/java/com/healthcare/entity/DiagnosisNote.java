package com.healthcare.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/** Clinical diagnosis note written by a doctor. */
@Entity
@Table(
    name = "diagnosis_notes",
    indexes = {
        @Index(name = "idx_diagnosis_patient", columnList = "patient_id"),
        @Index(name = "idx_diagnosis_doctor", columnList = "doctor_id")
    }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisNote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diagnosisText;

    /** ICD-10 code (e.g., J06.9 for Upper Respiratory Infection). */
    @Column(length = 20)
    private String icdCode;

    @Column(columnDefinition = "TEXT")
    private String treatmentPlan;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
