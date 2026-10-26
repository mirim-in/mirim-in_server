package com.mirim.mirimin.notice.service;

import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.service.AdminLogService;
import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.global.response.PageResponse;
import com.mirim.mirimin.notice.dto.NoticeDetailResponse;
import com.mirim.mirimin.notice.dto.NoticeRequest;
import com.mirim.mirimin.notice.dto.NoticeSummaryResponse;
import com.mirim.mirimin.notice.entity.Notice;
import com.mirim.mirimin.notice.entity.NoticeStatus;
import com.mirim.mirimin.notice.repository.NoticeRepository;
import com.mirim.mirimin.notification.service.NotificationService;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.service.UserReader;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** 공지 (AD-014, AD-015, NT-004). 작성·수정·삭제는 관리자만, 조회는 누구나 */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private static final String NOTICE = "NOTICE";

    private final NoticeRepository noticeRepository;
    private final NotificationService notificationService;
    private final AdminLogService adminLogService;
    private final UserReader userReader;
    private final Clock clock;

    // ===== 사용자 =====

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> getPublished(int page, int size) {
        return PageResponse.of(noticeRepository.findPublished(NoticeStatus.PUBLISHED, PageRequest.of(page, size)),
                NoticeSummaryResponse::of);
    }

    @Transactional(readOnly = true)
    public List<NoticeSummaryResponse> getPinned() {
        return noticeRepository.findByDeletedAtIsNullAndStatusAndPinnedTrueOrderByPublishedAtDesc(NoticeStatus.PUBLISHED)
                .stream().map(NoticeSummaryResponse::of).toList();
    }

    /** 임시 보관 공지는 관리자만 볼 수 있다. */
    @Transactional(readOnly = true)
    public NoticeDetailResponse getDetail(Long noticeId, AuthUser authUser) {
        Notice notice = getNotice(noticeId);
        if (notice.isDraft() && (authUser == null || !authUser.isAdmin())) {
            throw new BusinessException(ErrorCode.NOTICE_NOT_FOUND);
        }
        return NoticeDetailResponse.of(notice);
    }

    // ===== 관리자 =====

    @Transactional(readOnly = true)
    public PageResponse<NoticeSummaryResponse> searchForAdmin(NoticeStatus status, String keyword, int page, int size) {
        String normalizedKeyword = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return PageResponse.of(noticeRepository.searchForAdmin(status, normalizedKeyword,
                        PageRequest.of(page, size, Sort.by(Sort.Order.desc("id")))),
                NoticeSummaryResponse::of);
    }

    /** 바로 등록하면 전체 회원에게 알림이 간다. */
    @Transactional
    public Long create(NoticeRequest request, AuthUser authAdmin) {
        User admin = userReader.getActiveUser(authAdmin);
        Notice notice = noticeRepository.save(Notice.builder()
                .author(admin)
                .title(request.title().trim())
                .content(request.content())
                .pinned(request.pinned())
                .draft(request.draft())
                .now(LocalDateTime.now(clock))
                .build());
        notice.changeImages(request.imageUrlsOrEmpty());

        adminLogService.record(admin.getId(), AdminAction.CREATE_NOTICE, NOTICE, notice.getId(), notice.getTitle());
        if (!notice.isDraft()) {
            notificationService.notifyNotice(notice);
        }
        return notice.getId();
    }

    /** 임시 보관 공지를 draft=false로 수정하면 등록되고 그때 알림이 간다. 등록된 공지를 수정할 때는 다시 보내지 않는다. */
    @Transactional
    public void update(Long noticeId, NoticeRequest request, AuthUser authAdmin) {
        Notice notice = getNotice(noticeId);
        boolean wasDraft = notice.isDraft();
        notice.update(request.title().trim(), request.content(), request.pinned());
        notice.changeImages(request.imageUrlsOrEmpty());
        if (!request.draft()) {
            notice.publish(LocalDateTime.now(clock));
        }

        adminLogService.record(authAdmin.id(), AdminAction.UPDATE_NOTICE, NOTICE, noticeId, notice.getTitle());
        if (wasDraft && !notice.isDraft()) {
            notificationService.notifyNotice(notice);
        }
    }

    @Transactional
    public void delete(Long noticeId, AuthUser authAdmin) {
        Notice notice = getNotice(noticeId);
        notice.delete(LocalDateTime.now(clock));
        adminLogService.record(authAdmin.id(), AdminAction.DELETE_NOTICE, NOTICE, noticeId, notice.getTitle());
    }

    private Notice getNotice(Long noticeId) {
        return noticeRepository.findByIdAndDeletedAtIsNull(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));
    }
}
