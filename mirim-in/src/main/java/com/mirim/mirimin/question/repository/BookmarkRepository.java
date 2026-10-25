package com.mirim.mirimin.question.repository;

import com.mirim.mirimin.question.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<Bookmark> findByUserIdAndQuestionId(Long userId, Long questionId);

    /** 삭제된 질문은 빼고 최근에 담은 순 */
    @Query(value = """
            select b from Bookmark b join fetch b.question q
            where b.user.id = :userId and q.deletedAt is null
            order by b.id desc
            """,
            countQuery = """
            select count(b) from Bookmark b
            where b.user.id = :userId and b.question.deletedAt is null
            """)
    Page<Bookmark> findMyBookmarks(Long userId, Pageable pageable);
}
