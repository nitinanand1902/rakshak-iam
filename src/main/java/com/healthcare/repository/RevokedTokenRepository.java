package com.healthcare.repository;

import com.healthcare.entity.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Repository for managing revoked JWT tokens. */
public interface RevokedTokenRepository extends JpaRepository<RevokedToken, Long> {

    boolean existsByToken(String token);

    /** Purge expired revoked tokens to keep the table small. */
    @Modifying
    @Transactional
    @Query("DELETE FROM RevokedToken rt WHERE rt.expiresAt < :now")
    void deleteExpiredTokens(LocalDateTime now);
}
