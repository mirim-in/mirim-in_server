package com.mirim.mirimin.comment.entity;

import com.mirim.mirimin.answer.entity.Answer;
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

/**
 * 질문 댓글: answer == null
 * 답변 댓글: answer != null (question은 답변이 속한 질문)
 */
@Entity
@Table(name = "comments", indexes = {
        @Index(name = "idx_comment_question", columnList = "question_id"),
        @Index(name = "idx_comment_answer", columnList = "answer_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id")
    private Answer answer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 1000)
    private String content;

    private LocalDateTime deletedAt;

    private Comment(Question question, Answer answer, User author, String content) {
        this.question = question;
        this.answer = answer;
        this.author = author;
        this.content = content;
    }

    public static Comment onQuestion(Question question, User author, String content) {
        return new Comment(question, null, author, content);
    }

    public static Comment onAnswer(Answer answer, User author, String content) {
        return new Comment(answer.getQuestion(), answer, author, content);
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

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
