package com.healthcare.repository;

import com.healthcare.entity.DiagnosisNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Diagnosis note repository. */
public interface DiagnosisNoteRepository extends JpaRepository<DiagnosisNote, Long> {

    Page<DiagnosisNote> findByPatientId(Long patientId, Pageable pageable);

    Page<DiagnosisNote> findByDoctorId(Long doctorId, Pageable pageable);
}
