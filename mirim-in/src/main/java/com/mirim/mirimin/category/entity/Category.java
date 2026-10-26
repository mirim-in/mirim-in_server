package com.mirim.mirimin.category.entity;

import com.mirim.mirimin.global.entity.BaseTimeEntity;
import com.mirim.mirimin.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 카테고리 = 키워드.
 * official: 관리자가 만든 것. 목록 필터 칩과 답변 가능 분야 선택지에 노출된다.
 * 사용자가 질문 작성 중 "+추가"로 만든 것은 official=false이고 해당 질문에만 붙는다.
 * 이름이 같으면 새로 만들지 않고 기존 카테고리를 재사용한다.
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false)
    private boolean official;

    // 공식 카테고리 칩 노출 순서
    @Column(nullable = false)
    private int sortOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    private LocalDateTime deletedAt;

    private Category(String name, boolean official, int sortOrder, User createdBy) {
        this.name = name;
        this.official = official;
        this.sortOrder = sortOrder;
        this.createdBy = createdBy;
    }

    public static Category official(String name, int sortOrder, User admin) {
        return new Category(name, true, sortOrder, admin);
    }

    public static Category userCreated(String name, User user) {
        return new Category(name, false, 0, user);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void rename(String name) {
        this.name = name;
    }

    public void changeSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void promoteToOfficial(int sortOrder) {
        this.official = true;
        this.sortOrder = sortOrder;
    }

    public void delete(LocalDateTime now) {
        this.deletedAt = now;
    }

    /** 삭제했던 이름을 관리자가 다시 공식 카테고리로 추가할 때 */
    public void restoreAsOfficial(int sortOrder) {
        this.deletedAt = null;
        promoteToOfficial(sortOrder);
    }
}
