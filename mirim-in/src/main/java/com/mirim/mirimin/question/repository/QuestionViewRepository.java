package com.mirim.mirimin.question.repository;

import com.mirim.mirimin.question.entity.QuestionView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface QuestionViewRepository extends JpaRepository<QuestionView, Long> {

    boolean existsByQuestionIdAndViewerKeyAndViewedAtAfter(Long questionId, String viewerKey, LocalDateTime after);
}
