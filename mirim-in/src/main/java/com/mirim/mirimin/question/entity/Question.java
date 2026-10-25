package com.mirim.mirimin.question.entity;

import com.mirim.mirimin.category.entity.Category;
import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "questions", indexes = {
        @Index(name = "idx_question_status_created", columnList = "status, created_at"),
        @Index(name = "idx_question_author", columnList = "author_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 익명이어도 신고 대응을 위해 작성자는 항상 기록한다
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean anonymous;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private QuestionStatus status;

    // 목록 표시용 집계 값 (원본: question_views, answers, question_curiosities)
    @Column(nullable = false)
    private int viewCount;

    @Column(nullable = false)
    private int answerCount;

    @Column(nullable = false)
    private int curiosityCount;

    private LocalDateTime publishedAt;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionCategory> categories = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<QuestionImage> images = new ArrayList<>();

    @Builder
    private Question(User author, String title, String content, boolean anonymous, boolean draft, LocalDateTime now) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.anonymous = anonymous;
        this.status = draft ? QuestionStatus.DRAFT : QuestionStatus.OPEN;
        this.publishedAt = draft ? null : now;
    }

    public boolean isDraft() {
        return status == QuestionStatus.DRAFT;
    }

    public boolean isResolved() {
        return status == QuestionStatus.RESOLVED;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isAuthor(User user) {
        return author.getId().equals(user.getId());
    }

    public void update(String title, String content, boolean anonymous) {
        this.title = title;
        this.content = content;
        this.anonymous = anonymous;
    }

    /**
     * 기존 연결을 전부 지우고 다시 넣으면 flush 순서상 INSERT가 DELETE보다 먼저 나가
     * (question_id, category_id) unique 제약에 걸린다. 그래서 바뀐 것만 반영한다.
     */
    public void changeCategories(List<Category> newCategories) {
        Set<Long> newIds = newCategories.stream().map(Category::getId).collect(Collectors.toSet());
        categories.removeIf(qc -> !newIds.contains(qc.getCategory().getId()));
        Set<Long> currentIds = categories.stream().map(qc -> qc.getCategory().getId()).collect(Collectors.toSet());
        newCategories.stream()
                .filter(c -> !currentIds.contains(c.getId()))
                .forEach(c -> categories.add(new QuestionCategory(this, c)));
    }

    public void changeImages(List<String> imageUrls) {
        images.clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new QuestionImage(this, imageUrls.get(i), i));
        }
    }

    public void publish(LocalDateTime now) {
        if (isDraft()) {
            this.status = QuestionStatus.OPEN;
            this.publishedAt = now;
        }
    }

    public void resolve() {
        this.status = QuestionStatus.RESOLVED;
    }

    public void unresolve() {
        this.status = QuestionStatus.OPEN;
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void increaseAnswerCount() {
        this.answerCount++;
    }

    public void decreaseAnswerCount() {
        this.answerCount = Math.max(0, this.answerCount - 1);
    }

    public void increaseCuriosityCount() {
        this.curiosityCount++;
    }

    public void decreaseCuriosityCount() {
        this.curiosityCount = Math.max(0, this.curiosityCount - 1);
    }
}
