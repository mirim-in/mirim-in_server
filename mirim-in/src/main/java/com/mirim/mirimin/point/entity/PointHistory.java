package com.mirim.mirimin.point.entity;

import com.mirim.mirimin.user.entity.User;
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
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/** 포인트 증감 내역. users.point는 이 내역의 합계를 캐시한 값이다. */
@Entity
@Table(name = "point_histories", indexes = {
        @Index(name = "idx_point_history_user", columnList = "user_id, created_at"),
        @Index(name = "idx_point_history_reference", columnList = "reference_id, reason")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // 음수면 차감
    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 30)
    private PointReason reason;

    // 질문/답변 id (회수할 때 지급 내역을 찾는 데 쓴다)
    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 200)
    private String memo;

    // 관리자 수동 조정일 때만
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PointHistory(User user, int amount, PointReason reason, Long referenceId, String memo,
                         User admin, LocalDateTime now) {
        this.user = user;
        this.amount = amount;
        this.reason = reason;
        this.referenceId = referenceId;
        this.memo = memo;
        this.admin = admin;
        this.createdAt = now;
    }
}
