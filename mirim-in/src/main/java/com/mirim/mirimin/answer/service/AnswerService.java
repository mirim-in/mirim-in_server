package com.mirim.mirimin.answer.service;

import com.mirim.mirimin.answer.dto.AnswerRequest;
import com.mirim.mirimin.answer.dto.AnswerResponse;
import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.answer.entity.AnswerVote;
import com.mirim.mirimin.answer.entity.VoteType;
import com.mirim.mirimin.answer.repository.AnswerRepository;
import com.mirim.mirimin.answer.repository.AnswerVoteRepository;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.repository.QuestionRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import com.mirim.mirimin.user.service.UserTagReader;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnswerService {

    private final AnswerRepository answerRepository;
    private final AnswerVoteRepository answerVoteRepository;
    private final QuestionRepository questionRepository;
    private final UserReader userReader;
    private final UserTagReader userTagReader;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<AnswerResponse> getAnswers(Long questionId, AuthUser authUser) {
        Question question = getVisibleQuestion(questionId, authUser);
        List<Answer> answers = answerRepository.findForQuestion(question.getId());

        Map<Long, VoteType> myVotes = authUser == null ? Map.of() : answerVoteRepository
                .findByUserIdAndAnswerIdIn(authUser.id(), answers.stream().map(Answer::getId).toList())
                .stream()
                .collect(Collectors.toMap(v -> v.getAnswer().getId(), AnswerVote::getType));

        Map<Long, UserTagReader.UserTags> authorTags = userTagReader.readAll(
                answers.stream().map(a -> a.getAuthor().getId()).distinct().toList());

        return answers.stream()
                .map(a -> AnswerResponse.of(a, myVotes.get(a.getId()),
                        authUser != null && a.getAuthor().getId().equals(authUser.id()),
                        authorTags.getOrDefault(a.getAuthor().getId(), UserTagReader.UserTags.EMPTY)))
                .toList();
    }

    /** 학교 구성원 인증을 받은 사용자만 답변할 수 있다. */
    @Transactional
    public Long create(Long questionId, AnswerRequest request, AuthUser authUser) {
        User author = userReader.getWritableUser(authUser);
        if (!author.isVerifiedMember()) {
            throw new BusinessException(ErrorCode.NOT_VERIFIED_MEMBER);
        }
        Question question = questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .filter(q -> !q.isDraft())
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));

        Answer answer = answerRepository.save(new Answer(question, author, request.content()));
        Long answerId = answer.getId();
        questionRepository.addAnswerCount(question.getId(), 1);
        return answerId;
    }

    @Transactional
    public void update(Long answerId, AnswerRequest request, AuthUser authUser) {
        User user = userReader.getWritableUser(authUser);
        Answer answer = getAnswer(answerId);
        if (!answer.isAuthor(user)) {
            throw new BusinessException(ErrorCode.NOT_ANSWER_AUTHOR);
        }
        answer.update(request.content());
    }

    /**
     * 작성자 또는 관리자. 채택된 답변은 작성자가 지울 수 없다.
     * 관리자가 채택된 답변을 지우면 질문은 다시 답변 대기 상태가 된다.
     */
    @Transactional
    public void delete(Long answerId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Answer answer = getAnswer(answerId);
        boolean admin = user.isAdmin();
        if (!answer.isAuthor(user) && !admin) {
            throw new BusinessException(ErrorCode.NOT_ANSWER_AUTHOR);
        }
        if (answer.isAccepted()) {
            if (!admin) {
                throw new BusinessException(ErrorCode.CANNOT_DELETE_ACCEPTED_ANSWER);
            }
            answer.getQuestion().unresolve();
        }
        answer.delete(LocalDateTime.now(clock));
        questionRepository.addAnswerCount(answer.getQuestion().getId(), -1);
    }

    /** 질문 작성자만, 질문당 한 번. 본인 답변은 채택할 수 없다. */
    @Transactional
    public void accept(Long answerId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Answer answer = getAnswer(answerId);
        Question question = answer.getQuestion();
        if (question.isDeleted()) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        if (!question.isAuthor(user)) {
            throw new BusinessException(ErrorCode.NOT_QUESTION_AUTHOR_TO_ACCEPT);
        }
        if (question.isResolved()) {
            throw new BusinessException(ErrorCode.ALREADY_ACCEPTED);
        }
        if (answer.isAuthor(user)) {
            throw new BusinessException(ErrorCode.CANNOT_ACCEPT_OWN_ANSWER);
        }
        answer.accept(LocalDateTime.now(clock));
        question.resolve();
    }

    /** 추천/비추천 중 하나. 이미 다른 쪽을 눌렀으면 바꾼다. */
    @Transactional
    public void vote(Long answerId, VoteType type, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        Answer answer = getAnswer(answerId);
        if (answer.isAuthor(user)) {
            throw new BusinessException(ErrorCode.CANNOT_VOTE_OWN_ANSWER);
        }

        AnswerVote existing = answerVoteRepository.findByUserIdAndAnswerId(user.getId(), answerId).orElse(null);
        if (existing == null) {
            answerVoteRepository.save(new AnswerVote(user, answer, type));
            applyDelta(answerId, type, 1);
        } else if (existing.getType() != type) {
            VoteType before = existing.getType();
            existing.changeType(type);
            answerRepository.addVoteCount(answerId,
                    deltaFor(VoteType.LIKE, before, type), deltaFor(VoteType.DISLIKE, before, type));
        }
    }

    @Transactional
    public void cancelVote(Long answerId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        answerVoteRepository.findByUserIdAndAnswerId(user.getId(), answerId).ifPresent(vote -> {
            VoteType type = vote.getType();
            answerVoteRepository.delete(vote);
            applyDelta(answerId, type, -1);
        });
    }

    private void applyDelta(Long answerId, VoteType type, int delta) {
        answerRepository.addVoteCount(answerId, type == VoteType.LIKE ? delta : 0, type == VoteType.DISLIKE ? delta : 0);
    }

    // before → after 로 바뀔 때 target 집계의 증감
    private int deltaFor(VoteType target, VoteType before, VoteType after) {
        return (after == target ? 1 : 0) - (before == target ? 1 : 0);
    }

    /** 임시 보관 질문의 답변 목록은 작성자만 볼 수 있다. (실제로는 답변이 달릴 수 없다) */
    private Question getVisibleQuestion(Long questionId, AuthUser authUser) {
        Question question = questionRepository.findByIdAndDeletedAtIsNull(questionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.QUESTION_NOT_FOUND));
        boolean mine = authUser != null && question.getAuthor().getId().equals(authUser.id());
        if (question.isDraft() && !mine) {
            throw new BusinessException(ErrorCode.QUESTION_NOT_FOUND);
        }
        return question;
    }

    private Answer getAnswer(Long answerId) {
        return answerRepository.findByIdAndDeletedAtIsNull(answerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ANSWER_NOT_FOUND));
    }
}
