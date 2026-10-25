package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.EmailVerification;
import com.mirim.mirimin.user.entity.VerificationPurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    Optional<EmailVerification> findTopByEmailAndPurposeOrderByIdDesc(String email, VerificationPurpose purpose);

    Optional<EmailVerification> findByCodeAndPurpose(String code, VerificationPurpose purpose);

    @Modifying(clearAutomatically = true)
    @Query("delete from EmailVerification v where v.email = :email and v.purpose = :purpose")
    void deleteAllByEmailAndPurpose(String email, VerificationPurpose purpose);
}
