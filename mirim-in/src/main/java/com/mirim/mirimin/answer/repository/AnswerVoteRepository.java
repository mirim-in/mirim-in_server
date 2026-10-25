package com.mirim.mirimin.answer.repository;

import com.mirim.mirimin.answer.entity.AnswerVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AnswerVoteRepository extends JpaRepository<AnswerVote, Long> {

    Optional<AnswerVote> findByUserIdAndAnswerId(Long userId, Long answerId);

    List<AnswerVote> findByUserIdAndAnswerIdIn(Long userId, Collection<Long> answerIds);
}
