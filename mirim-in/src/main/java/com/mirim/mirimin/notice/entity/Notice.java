package com.mirim.mirimin.notice.entity;

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

@Entity
@Table(name = "notices", indexes = @Index(name = "idx_notice_status_pinned", columnList = "status, pinned"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private NoticeStatus status;

    // 질문 목록 상단 고정
    @Column(nullable = false)
    private boolean pinned;

    private LocalDateTime publishedAt;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "notice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    private List<NoticeImage> images = new ArrayList<>();

    @Builder
    private Notice(User author, String title, String content, boolean pinned, boolean draft, LocalDateTime now) {
        this.author = author;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.status = draft ? NoticeStatus.DRAFT : NoticeStatus.PUBLISHED;
        this.publishedAt = draft ? null : now;
    }

    public boolean isDraft() {
        return status == NoticeStatus.DRAFT;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void update(String title, String content, boolean pinned) {
        this.title = title;
        this.content = content;
        this.pinned = pinned;
    }

    public void changeImages(List<String> imageUrls) {
        images.clear();
        for (int i = 0; i < imageUrls.size(); i++) {
            images.add(new NoticeImage(this, imageUrls.get(i), i));
        }
    }

    public void publish(LocalDateTime now) {
        if (isDraft()) {
            this.status = NoticeStatus.PUBLISHED;
            this.publishedAt = now;
        }
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }
}
