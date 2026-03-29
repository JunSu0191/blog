package com.study.blog.verification;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VerificationCodeRepository extends JpaRepository<VerificationCode, Long> {

    Optional<VerificationCode> findTopByTargetAndPurposeOrderByCreatedAtDesc(String target, VerificationPurpose purpose);
}
