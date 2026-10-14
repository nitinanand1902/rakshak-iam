package com.healthcare.repository;

import com.healthcare.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Core user repository. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Looks up an SSO-provisioned user by their IdP subject identifier. */
    Optional<User> findByProviderSub(String providerSub);

    boolean existsByProviderSub(String providerSub);
}
