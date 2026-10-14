package com.healthcare.repository;

import com.healthcare.entity.ActiveSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Repository for active JWT sessions. */
public interface ActiveSessionRepository extends JpaRepository<ActiveSession, Long> {

    List<ActiveSession> findByUserId(Long userId);

    @Modifying
    @Transactional
    void deleteByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM ActiveSession s WHERE s.expiresAt < :now")
    void deleteExpiredSessions(LocalDateTime now);
}
