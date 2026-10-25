package com.mirim.mirimin.answer.repository;

import com.mirim.mirimin.answer.entity.Answer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AnswerRepository extends JpaRepository<Answer, Long> {

    Optional<Answer> findByIdAndDeletedAtIsNull(Long id);

    /** 채택된 답변 → 추천 많은 순 → 먼저 쓴 순 */
    @Query("""
            select a from Answer a
            where a.question.id = :questionId and a.deletedAt is null
            order by a.accepted desc, a.likeCount desc, a.createdAt asc, a.id asc
            """)
    List<Answer> findForQuestion(Long questionId);

    // 추천 수는 동시에 바뀔 수 있고, 엔티티로 올리면 updatedAt(수정 시각)이 바뀌므로 DB에서 직접 증감한다
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Answer a
            set a.likeCount = a.likeCount + :likeDelta, a.dislikeCount = a.dislikeCount + :dislikeDelta
            where a.id = :id
            """)
    void addVoteCount(Long id, int likeDelta, int dislikeDelta);
}
