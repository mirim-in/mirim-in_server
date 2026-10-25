package com.mirim.mirimin.answer.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.question.entity.Question;
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

@Entity
@Table(name = "answers", indexes = {
        @Index(name = "idx_answer_question", columnList = "question_id"),
        @Index(name = "idx_answer_author", columnList = "author_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean accepted;

    private LocalDateTime acceptedAt;

    // 정렬용 집계 값 (원본: answer_votes)
    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int dislikeCount;

    private LocalDateTime deletedAt;

    public Answer(Question question, User author, String content) {
        this.question = question;
        this.author = author;
        this.content = content;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAuthor(User user) {
        return author.getId().equals(user.getId());
    }

    public void update(String content) {
        this.content = content;
    }

    public void accept(LocalDateTime now) {
        this.accepted = true;
        this.acceptedAt = now;
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    /** delta: +1(투표), -1(취소) */
    public void applyVote(VoteType type, int delta) {
        if (type == VoteType.LIKE) {
            this.likeCount = Math.max(0, this.likeCount + delta);
        } else {
            this.dislikeCount = Math.max(0, this.dislikeCount + delta);
        }
    }
}
