package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemberVerificationRequestRepository extends JpaRepository<MemberVerificationRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, VerificationRequestStatus status);

    List<MemberVerificationRequest> findByUserIdOrderByIdDesc(Long userId);
}
