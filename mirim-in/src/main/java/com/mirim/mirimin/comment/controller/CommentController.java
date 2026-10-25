package com.mirim.mirimin.comment.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.dto.CommentRequest;
import com.mirim.mirimin.comment.dto.CommentResponse;
import com.mirim.mirimin.comment.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "댓글")
@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "질문 댓글 목록", description = "오래된 순")
    @GetMapping("/api/questions/{questionId}/comments")
    public List<CommentResponse> getQuestionComments(@PathVariable Long questionId,
                                                     @AuthenticationPrincipal AuthUser authUser) {
        return commentService.getQuestionComments(questionId, authUser);
    }

    @Operation(summary = "답변 댓글 목록", description = "오래된 순")
    @GetMapping("/api/answers/{answerId}/comments")
    public List<CommentResponse> getAnswerComments(@PathVariable Long answerId,
                                                   @AuthenticationPrincipal AuthUser authUser) {
        return commentService.getAnswerComments(answerId, authUser);
    }

    @Operation(summary = "질문에 댓글 작성", description = "로그인한 사용자 누구나. 응답은 생성된 댓글 id")
    @PostMapping("/api/questions/{questionId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createOnQuestion(@PathVariable Long questionId, @Valid @RequestBody CommentRequest request,
                                 @AuthenticationPrincipal AuthUser authUser) {
        return commentService.createOnQuestion(questionId, request, authUser);
    }

    @Operation(summary = "답변에 댓글 작성", description = "로그인한 사용자 누구나. 응답은 생성된 댓글 id")
    @PostMapping("/api/answers/{answerId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Long createOnAnswer(@PathVariable Long answerId, @Valid @RequestBody CommentRequest request,
                               @AuthenticationPrincipal AuthUser authUser) {
        return commentService.createOnAnswer(answerId, request, authUser);
    }

    @Operation(summary = "댓글 수정", description = "작성자만")
    @PutMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long commentId, @Valid @RequestBody CommentRequest request,
                       @AuthenticationPrincipal AuthUser authUser) {
        commentService.update(commentId, request, authUser);
    }

    @Operation(summary = "댓글 삭제", description = "작성자 또는 관리자")
    @DeleteMapping("/api/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long commentId, @AuthenticationPrincipal AuthUser authUser) {
        commentService.delete(commentId, authUser);
    }
}
