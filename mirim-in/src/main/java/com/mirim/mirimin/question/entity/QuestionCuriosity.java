package com.mirim.mirimin.question.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.Entity;
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

/** "저도 궁금해요". 누른 사용자도 새 답변 알림을 받는다. */
@Entity
@Table(name = "question_curiosities", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "question_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionCuriosity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    public QuestionCuriosity(User user, Question question) {
        this.user = user;
        this.question = question;
    }
}
