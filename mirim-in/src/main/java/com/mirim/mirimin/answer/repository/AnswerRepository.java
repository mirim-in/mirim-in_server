package com.mirim.mirimin.answer.repository;

import com.mirim.mirimin.answer.entity.Answer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNullAndQuestionDeletedAtIsNull();

    /** 채택된 답변 → 추천 많은 순 → 먼저 쓴 순 */
    @Query("""
            select a from Answer a
            where a.question.id = :questionId and a.deletedAt is null
            order by a.accepted desc, a.likeCount desc, a.createdAt asc, a.id asc
            """)
    List<Answer> findForQuestion(Long questionId);

    /** 프로필 > 답변 목록, 대표 답변. 삭제된 질문의 답변은 뺀다 */
    @Query(value = """
            select a from Answer a join fetch a.question q
            where a.author.id = :authorId and a.deletedAt is null and q.deletedAt is null
            """,
            countQuery = """
            select count(a) from Answer a
            where a.author.id = :authorId and a.deletedAt is null and a.question.deletedAt is null
            """)
    Page<Answer> findByAuthor(Long authorId, Pageable pageable);

    /** [답변 수, 채택 수, 받은 추천 수] */
    @Query("""
            select count(a),
                   coalesce(sum(case when a.accepted = true then 1 else 0 end), 0),
                   coalesce(sum(a.likeCount), 0)
            from Answer a
            where a.author.id = :authorId and a.deletedAt is null and a.question.deletedAt is null
            """)
    List<Object[]> findStats(Long authorId);

    // 추천 수는 동시에 바뀔 수 있고, 엔티티로 올리면 updatedAt(수정 시각)이 바뀌므로 DB에서 직접 증감한다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Answer a
            set a.likeCount = a.likeCount + :likeDelta, a.dislikeCount = a.dislikeCount + :dislikeDelta
            where a.id = :id
            """)
    void addVoteCount(Long id, int likeDelta, int dislikeDelta);
}
