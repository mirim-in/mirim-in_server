package com.mirim.mirimin.notice.dto;

import com.mirim.mirimin.notice.entity.Notice;
import com.mirim.mirimin.notice.entity.NoticeStatus;

import java.time.LocalDateTime;

/** 공지 목록 항목 (질문 목록 상단 고정 공지, 사이드바 공지 탭, 관리자 공지 목록) */
public record NoticeSummaryResponse(
        Long id,
        String title,
        boolean pinned,
        NoticeStatus status,
        String authorName,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {

    public static NoticeSummaryResponse of(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getTitle(),
                notice.isPinned(),
                notice.getStatus(),
                notice.getAuthor().getName(),
                notice.getPublishedAt(),
                notice.getUpdatedAt()
        );
    }
}
