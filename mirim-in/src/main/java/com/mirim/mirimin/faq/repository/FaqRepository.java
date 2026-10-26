package com.mirim.mirimin.faq.repository;

import com.mirim.mirimin.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FaqRepository extends JpaRepository<Faq, Long> {

    /** categoryId가 null이면 전체. 노출 순서대로 */
    @Query("""
            select f from Faq f left join fetch f.category c
            where (:categoryId is null or c.id = :categoryId)
            order by f.sortOrder asc, f.id asc
            """)
    List<Faq> findAllByCategory(Long categoryId);
}
