package com.project.rentmebarwaha.auth.repository;

import com.project.rentmebarwaha.auth.entity.OtpToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpTokenRepository extends JpaRepository<OtpToken, Long> {

    long countByTargetAndCreatedAtAfter(String target, Instant after);

    Optional<OtpToken> findFirstByTargetAndUsedFalseAndExpiresAtAfterOrderByCreatedAtDesc(String target, Instant now);
}

