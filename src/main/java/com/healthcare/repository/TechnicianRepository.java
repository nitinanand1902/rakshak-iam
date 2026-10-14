package com.healthcare.repository;

import com.healthcare.entity.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Technician profile repository. */
public interface TechnicianRepository extends JpaRepository<Technician, Long> {

    Optional<Technician> findByUserUsername(String username);

    Optional<Technician> findByUserId(Long userId);
}
