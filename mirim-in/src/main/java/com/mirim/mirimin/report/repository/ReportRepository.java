package com.mirim.mirimin.report.repository;

import com.mirim.mirimin.report.entity.Report;
import com.mirim.mirimin.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);
}
