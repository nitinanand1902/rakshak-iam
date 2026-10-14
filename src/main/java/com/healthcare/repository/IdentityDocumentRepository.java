package com.healthcare.repository;

import com.healthcare.entity.IdentityDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

/** Repository for identity documents. */
public interface IdentityDocumentRepository extends JpaRepository<IdentityDocument, Long> {

    List<IdentityDocument> findByPatientId(Long patientId);

    @Query("SELECT d FROM IdentityDocument d WHERE d.patient.user.id = :userId")
    List<IdentityDocument> findByPatientUserId(Long userId);
}
