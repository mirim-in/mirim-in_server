package com.mirim.mirimin.user.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
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
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** 학교 이메일이 없는 사용자(졸업생 등)의 미림인 승인 신청 */
@Entity
@Table(name = "member_verification_requests", indexes = @Index(name = "idx_mvr_status", columnList = "status"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MemberVerificationRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private MemberType requestedType;

    @Column(length = 500)
    private String message;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private VerificationRequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private User reviewer;

    private LocalDateTime reviewedAt;

    @Column(length = 500)
    private String rejectReason;

    public MemberVerificationRequest(User user, MemberType requestedType, String message) {
        this.user = user;
        this.requestedType = requestedType;
        this.message = message;
        this.status = VerificationRequestStatus.PENDING;
    }

    public void approve(User reviewer, LocalDateTime now) {
        this.status = VerificationRequestStatus.APPROVED;
        this.reviewer = reviewer;
        this.reviewedAt = now;
        this.user.verifyAs(requestedType);
    }

    public void reject(User reviewer, String rejectReason, LocalDateTime now) {
        this.status = VerificationRequestStatus.REJECTED;
        this.reviewer = reviewer;
        this.reviewedAt = now;
        this.rejectReason = rejectReason;
    }
}
