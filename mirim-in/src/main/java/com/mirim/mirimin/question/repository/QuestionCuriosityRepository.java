package com.mirim.mirimin.question.repository;

import com.mirim.mirimin.question.entity.QuestionCuriosity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QuestionCuriosityRepository extends JpaRepository<QuestionCuriosity, Long> {

    boolean existsByUserIdAndQuestionId(Long userId, Long questionId);

    Optional<QuestionCuriosity> findByUserIdAndQuestionId(Long userId, Long questionId);
}
