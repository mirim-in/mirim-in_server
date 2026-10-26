package com.mirim.mirimin.notice.dto;

import com.mirim.mirimin.notice.entity.Notice;
import com.mirim.mirimin.notice.entity.NoticeImage;
import com.mirim.mirimin.notice.entity.NoticeStatus;

import java.time.LocalDateTime;
import java.util.List;

public record NoticeDetailResponse(
        Long id,
        String title,
        String content,
        List<String> imageUrls,
        boolean pinned,
        NoticeStatus status,
        String authorName,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {

    public static NoticeDetailResponse of(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                notice.getImages().stream().map(NoticeImage::getUrl).toList(),
                notice.isPinned(),
                notice.getStatus(),
                notice.getAuthor().getName(),
                notice.getPublishedAt(),
                notice.getUpdatedAt()
        );
    }
}
