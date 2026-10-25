package com.mirim.mirimin.user.repository;

import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MemberVerificationRequestRepository extends JpaRepository<MemberVerificationRequest, Long> {

    boolean existsByUserIdAndStatus(Long userId, VerificationRequestStatus status);

    List<MemberVerificationRequest> findByUserIdOrderByIdDesc(Long userId);

    long countByStatus(VerificationRequestStatus status);

    /** 관리자 승인 목록. 조건이 null이면 무시한다. keyword는 신청자 이름·이메일에서 찾는다. */
    @Query(value = """
            select r from MemberVerificationRequest r join fetch r.user u
            where (:status is null or r.status = :status)
              and (:keyword is null
                    or u.name like concat('%', :keyword, '%')
                    or u.email like concat('%', :keyword, '%'))
            """,
            countQuery = """
            select count(r) from MemberVerificationRequest r join r.user u
            where (:status is null or r.status = :status)
              and (:keyword is null
                    or u.name like concat('%', :keyword, '%')
                    or u.email like concat('%', :keyword, '%'))
            """)
    Page<MemberVerificationRequest> search(VerificationRequestStatus status, String keyword, Pageable pageable);
}
