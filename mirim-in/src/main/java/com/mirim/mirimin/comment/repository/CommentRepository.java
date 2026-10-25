package com.mirim.mirimin.comment.repository;

import com.mirim.mirimin.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    /** 질문에 달린 댓글 (답변 댓글 제외) */
    List<Comment> findByQuestionIdAndAnswerIsNullAndDeletedAtIsNullOrderByIdAsc(Long questionId);

    List<Comment> findByAnswerIdAndDeletedAtIsNullOrderByIdAsc(Long answerId);
}
