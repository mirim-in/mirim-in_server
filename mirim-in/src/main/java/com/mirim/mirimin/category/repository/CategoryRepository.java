package com.mirim.mirimin.category.repository;

import com.mirim.mirimin.category.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByOfficialTrueAndDeletedAtIsNullOrderBySortOrderAscIdAsc();

    Optional<Category> findByName(String name);

    /** 관리자 카테고리 목록. official이 null이면 공식·사용자 카테고리 모두 */
    @Query("""
            select c from Category c
            where c.deletedAt is null
              and (:official is null or c.official = :official)
              and (:keyword is null or c.name like concat('%', :keyword, '%'))
            """)
    Page<Category> searchForAdmin(Boolean official, String keyword, Pageable pageable);

    /** 카테고리별 질문 수 (삭제된 질문 제외). 결과: [categoryId, 질문 수] */
    @Query("""
            select qc.category.id, count(qc) from QuestionCategory qc
            where qc.category.id in :categoryIds and qc.question.deletedAt is null
            group by qc.category.id
            """)
    List<Object[]> countQuestions(Collection<Long> categoryIds);
}
