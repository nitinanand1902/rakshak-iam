package com.healthcare.repository;

import com.healthcare.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Doctor profile repository. */
public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByUserUsername(String username);

    Optional<Doctor> findByUserId(Long userId);
}
