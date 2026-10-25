package com.mirim.mirimin.question.repository;

import com.mirim.mirimin.question.entity.QuestionCuriosity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface QuestionCuriosityRepository extends JpaRepository<QuestionCuriosity, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<QuestionCuriosity> findByUserIdAndQuestionId(Long userId, Long questionId);

    /** 새 답변 알림 대상. 탈퇴한 사용자는 뺀다. */
    @Query("select c.user.id from QuestionCuriosity c where c.question.id = :questionId and c.user.deletedAt is null")
    List<Long> findUserIds(Long questionId);
}
