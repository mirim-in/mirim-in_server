package com.mirim.mirimin.answer.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** 답변 하나에 사용자당 추천 또는 비추천 하나 */
@Entity
@Table(name = "answer_votes", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "answer_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnswerVote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 10)
    private VoteType type;

    public AnswerVote(User user, Answer answer, VoteType type) {
        this.user = user;
        this.answer = answer;
        this.type = type;
    }

    public void changeType(VoteType type) {
        this.type = type;
    }
}
