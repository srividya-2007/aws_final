package com.ecogrid.auth.repository;

import com.ecogrid.auth.model.OtpPurpose;
import com.ecogrid.auth.model.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(String email, OtpPurpose purpose);

    List<OtpVerification> findAllByEmailAndPurposeAndUsedFalse(String email, OtpPurpose purpose);

    void deleteAllByEmail(String email);
}
