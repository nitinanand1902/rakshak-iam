package com.healthcare.repository;

import com.healthcare.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Patient profile repository. */
public interface PatientRepository extends JpaRepository<Patient, Long> {

    Optional<Patient> findByUserUsername(String username);

    Optional<Patient> findByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
