package com.mirim.mirimin.report.repository;

import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportStatus;
import com.mirim.mirimin.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByStatus(ReportStatus status);

    /** 회원별 받은 신고 수. 결과: [userId, 신고 수] */
    @Query("select r.targetUser.id, count(r) from Report r where r.targetUser.id in :userIds group by r.targetUser.id")
    List<Object[]> countByTargetUsers(Collection<Long> userIds);
}
