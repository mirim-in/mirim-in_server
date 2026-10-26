package com.mirim.mirimin.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 공지 작성·수정 공통.
 * pinned: 질문 목록 상단 고정
 * draft: true면 임시 보관. 등록되는 순간 전체 사용자에게 알림이 간다.
 */
public record NoticeRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 20000, message = "내용은 20,000자 이하로 입력해 주세요.")
        String content,

        @Size(max = 10, message = "이미지는 10장까지 첨부할 수 있어요.")
        List<@Pattern(regexp = "^https://res\\.cloudinary\\.com/.+", message = "업로드한 이미지 주소만 사용할 수 있어요.")
             String> imageUrls,

        boolean pinned,

        boolean draft
) {

    public List<String> imageUrlsOrEmpty() {
        return imageUrls == null ? List.of() : imageUrls;
    }
}
