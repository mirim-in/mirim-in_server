package com.mirim.mirimin.notification.service;

import com.mirim.mirimin.answer.entity.Answer;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.comment.entity.Comment;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.notification.dto.NotificationResponse;
import com.mirim.mirimin.notification.entity.Notification;
import com.mirim.mirimin.notification.entity.NotificationType;
import com.mirim.mirimin.notification.repository.NotificationRepository;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionCategory;
import com.mirim.mirimin.question.repository.QuestionCuriosityRepository;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.UserCategoryType;
import com.mirim.mirimin.user.repository.UserCategoryRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 알림 생성과 조회. 생성 메서드는 질문·답변·댓글 서비스의 같은 트랜잭션 안에서 호출된다.
 * 자기 행동으로 자기에게 알림이 가지 않는다.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final String ANONYMOUS_NAME = "익명";

    private final NotificationRepository notificationRepository;
    private final QuestionCuriosityRepository curiosityRepository;
    private final UserCategoryRepository userCategoryRepository;
    private final UserRepository userRepository;
    private final UserReader userReader;
    private final Clock clock;

    /** 새 답변 → 질문 작성자 + "저도 궁금해요"를 누른 사람 */
    @Transactional
    public void notifyAnswerCreated(Answer answer) {
        Question question = answer.getQuestion();
        Set<Long> receivers = new LinkedHashSet<>();
        receivers.add(question.getAuthor().getId());
        receivers.addAll(curiosityRepository.findUserIds(question.getId()));
        receivers.remove(answer.getAuthor().getId());

        send(receivers, NotificationType.ANSWER_CREATED, question,
                answer.getAuthor().getName() + "님이 답변을 남겼어요.");
    }

    /** 채택 → 답변 작성자 */
    @Transactional
    public void notifyAnswerAccepted(Answer answer) {
        send(List.of(answer.getAuthor().getId()), NotificationType.ANSWER_ACCEPTED, answer.getQuestion(),
                "내 답변이 채택되었어요.");
    }

    /** 댓글 → 질문 댓글이면 질문 작성자, 답변 댓글이면 답변 작성자 */
    @Transactional
    public void notifyCommentCreated(Comment comment) {
        Question question = comment.getQuestion();
        User target = comment.getAnswer() != null ? comment.getAnswer().getAuthor() : question.getAuthor();
        if (target.getId().equals(comment.getAuthor().getId())) {
            return;
        }
        boolean anonymousAuthor = question.isAnonymous()
                && question.getAuthor().getId().equals(comment.getAuthor().getId());
        String actor = anonymousAuthor ? ANONYMOUS_NAME : comment.getAuthor().getName();
        send(List.of(target.getId()), NotificationType.COMMENT_CREATED, question, actor + "님이 댓글을 남겼어요.");
    }

    /** 새 질문 → 질문의 카테고리를 답변 가능 분야로 설정한 사람 (QD-008, NT-003) */
    @Transactional
    public void notifyCategoryMatched(Question question) {
        List<Long> categoryIds = question.getCategories().stream()
                .map(QuestionCategory::getCategory)
                .filter(c -> c.isOfficial() && !c.isDeleted())
                .map(c -> c.getId())
                .toList();
        if (categoryIds.isEmpty()) {
            return;
        }
        Set<Long> receivers = new LinkedHashSet<>(
                userCategoryRepository.findUserIds(categoryIds, UserCategoryType.ANSWERABLE));
        receivers.remove(question.getAuthor().getId());

        send(receivers, NotificationType.CATEGORY_MATCHED, question, "답변 가능 분야에 새 질문이 올라왔어요.");
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(AuthUser authUser, int page, int size) {
        User user = userReader.getActiveUser(authUser);
        return PageResponse.of(notificationRepository.findByReceiverIdOrderByIdDesc(user.getId(),
                PageRequest.of(page, size)), NotificationResponse::of);
    }

    @Transactional(readOnly = true)
    public long countUnread(AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        return notificationRepository.countByReceiverIdAndReadAtIsNull(user.getId());
    }

    @Transactional
    public void markRead(Long notificationId, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        notificationRepository.findByIdAndReceiverId(notificationId, user.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND))
                .read(LocalDateTime.now(clock));
    }

    @Transactional
    public void markAllRead(AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        notificationRepository.markAllRead(user.getId(), LocalDateTime.now(clock));
    }

    private void send(Collection<Long> receiverIds, NotificationType type, Question question, String message) {
        LocalDateTime now = LocalDateTime.now(clock);
        for (Long receiverId : receiverIds) {
            notificationRepository.save(Notification.builder()
                    .receiver(userRepository.getReferenceById(receiverId))
                    .type(type)
                    .question(question)
                    .message(message)
                    .now(now)
                    .build());
        }
    }
}
