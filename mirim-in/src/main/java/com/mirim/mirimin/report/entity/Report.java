package com.mirim.mirimin.report.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Entity
@Table(name = "reports", indexes = {
        @Index(name = "idx_report_status_created", columnList = "status, created_at"),
        @Index(name = "idx_report_target_user", columnList = "target_user_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id")
    private User reporter;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    // 신고당한 글의 작성자 (반복 신고 집계, 정지 조치 대상)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_user_id")
    private User targetUser;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "report_reasons", joinColumns = @JoinColumn(name = "report_id"))
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "reason", nullable = false, length = 20)
    private Set<ReportReason> reasons = EnumSet.noneOf(ReportReason.class);

    @Column(length = 500)
    private String detail;

    // 원글이 수정·삭제돼도 관리자가 확인할 수 있도록 신고 시점 내용을 저장
    @Column(nullable = false, columnDefinition = "TEXT")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private ReportStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    private LocalDateTime processedAt;

    @Builder
    private Report(User reporter, ReportTargetType targetType, Long targetId, User targetUser,
                   Set<ReportReason> reasons, String detail, String contentSnapshot) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetUser = targetUser;
        this.reasons.addAll(reasons);
        this.detail = detail;
        this.contentSnapshot = contentSnapshot;
        this.status = ReportStatus.PENDING;
    }

    public void hold(User admin, LocalDateTime now) {
        changeStatus(ReportStatus.ON_HOLD, admin, now);
    }

    public void complete(User admin, LocalDateTime now) {
        changeStatus(ReportStatus.DONE, admin, now);
    }

    private void changeStatus(ReportStatus status, User admin, LocalDateTime now) {
        this.status = status;
        this.processedBy = admin;
        this.processedAt = now;
    }
}
