package com.healthcare.repository;

import com.healthcare.entity.DiagnosticReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiagnosticReportRepository extends JpaRepository<DiagnosticReport, Long> {
    List<DiagnosticReport> findByPatientIdOrderByCreatedAtDesc(Long patientId);
    List<DiagnosticReport> findByTechnicianIdOrderByCreatedAtDesc(Long technicianId);
}
