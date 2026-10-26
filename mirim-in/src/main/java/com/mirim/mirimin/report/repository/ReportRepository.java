package com.mirim.mirimin.report.repository;

import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportReason;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByStatus(ReportStatus status);

    /** 관리자 신고 목록. 조건이 null이면 무시한다. */
    @Query(value = """
            select r from Report r join fetch r.reporter join fetch r.targetUser
            where (:status is null or r.status = :status)
              and (:targetType is null or r.targetType = :targetType)
              and (:reason is null or :reason member of r.reasons)
            """,
            countQuery = """
            select count(r) from Report r
            where (:status is null or r.status = :status)
              and (:targetType is null or r.targetType = :targetType)
              and (:reason is null or :reason member of r.reasons)
            """)
    Page<Report> search(ReportStatus status, ReportTargetType targetType, ReportReason reason, Pageable pageable);

    /** 같은 글에 대한 미처리(대기·보류) 신고 */
    List<Report> findByTargetTypeAndTargetIdAndStatusIn(ReportTargetType targetType, Long targetId,
                                                         Collection<ReportStatus> statuses);

    /** 회원별 받은 신고 수. 결과: [userId, 신고 수] */
    @Query("select r.targetUser.id, count(r) from Report r where r.targetUser.id in :userIds group by r.targetUser.id")
    List<Object[]> countByTargetUsers(Collection<Long> userIds);
}
