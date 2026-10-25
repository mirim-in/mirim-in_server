package com.mirim.mirimin.answer.controller;

import com.mirim.mirimin.answer.dto.AnswerRequest;
import com.mirim.mirimin.answer.dto.AnswerResponse;
import com.mirim.mirimin.answer.dto.VoteRequest;
import com.mirim.mirimin.answer.service.AnswerService;
import com.mirim.mirimin.auth.jwt.AuthUser;
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

@Tag(name = "답변")
@RestController
@RequiredArgsConstructor
public class AnswerController {

    private final AnswerService answerService;

    @Operation(summary = "질문의 답변 목록", description = "채택된 답변 → 추천 많은 순 → 먼저 쓴 순")
    @GetMapping("/api/questions/{questionId}/answers")
    public List<AnswerResponse> getAnswers(@PathVariable Long questionId,
                                           @AuthenticationPrincipal AuthUser authUser) {
        return answerService.getAnswers(questionId, authUser);
    }

    @Operation(summary = "답변 작성", description = "학교 구성원 인증을 받은 사용자만. 응답은 생성된 답변 id")
    @PostMapping("/api/questions/{questionId}/answers")
    @ResponseStatus(HttpStatus.CREATED)
    public Long create(@PathVariable Long questionId, @Valid @RequestBody AnswerRequest request,
                       @AuthenticationPrincipal AuthUser authUser) {
        return answerService.create(questionId, request, authUser);
    }

    @Operation(summary = "답변 수정", description = "작성자만")
    @PutMapping("/api/answers/{answerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void update(@PathVariable Long answerId, @Valid @RequestBody AnswerRequest request,
                       @AuthenticationPrincipal AuthUser authUser) {
        answerService.update(answerId, request, authUser);
    }

    @Operation(summary = "답변 삭제", description = "작성자 또는 관리자. 채택된 답변은 작성자가 삭제할 수 없다.")
    @DeleteMapping("/api/answers/{answerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long answerId, @AuthenticationPrincipal AuthUser authUser) {
        answerService.delete(answerId, authUser);
    }

    @Operation(summary = "답변 채택", description = "질문 작성자만, 질문당 한 번. 본인 답변은 채택할 수 없다.")
    @PostMapping("/api/answers/{answerId}/accept")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void accept(@PathVariable Long answerId, @AuthenticationPrincipal AuthUser authUser) {
        answerService.accept(answerId, authUser);
    }

    @Operation(summary = "추천/비추천", description = "LIKE 또는 DISLIKE. 이미 반대쪽을 눌렀으면 바뀐다. 본인 답변은 불가")
    @PutMapping("/api/answers/{answerId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void vote(@PathVariable Long answerId, @Valid @RequestBody VoteRequest request,
                     @AuthenticationPrincipal AuthUser authUser) {
        answerService.vote(answerId, request.type(), authUser);
    }

    @Operation(summary = "추천/비추천 취소")
    @DeleteMapping("/api/answers/{answerId}/vote")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancelVote(@PathVariable Long answerId, @AuthenticationPrincipal AuthUser authUser) {
        answerService.cancelVote(answerId, authUser);
    }
}
