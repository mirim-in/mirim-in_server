package com.mirim.mirimin.question.entity;

import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

/**
 * 조회 기록. 인기 질문(이번 주/이번 달) 집계에 쓴다.
 * 같은 viewerKey가 같은 질문을 일정 시간 안에 다시 보면 기록하지 않는다(서비스에서 처리).
 */
@Entity
@Table(name = "question_views", indexes = {
        @Index(name = "idx_question_view_viewed_at", columnList = "viewed_at, question_id"),
        @Index(name = "idx_question_view_dedup", columnList = "question_id, viewer_key, viewed_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    // 비로그인 조회는 null
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 로그인: "u:{userId}", 비로그인: 클라이언트 식별값 해시
    @Column(name = "viewer_key", nullable = false, length = 64)
    private String viewerKey;

    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public QuestionView(Question question, User user, String viewerKey, LocalDateTime viewedAt) {
        this.question = question;
        this.user = user;
        this.viewerKey = viewerKey;
        this.viewedAt = viewedAt;
    }
}
