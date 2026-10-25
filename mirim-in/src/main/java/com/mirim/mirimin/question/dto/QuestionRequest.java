package com.mirim.mirimin.question.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 질문 작성·수정 공통.
 * categoryIds: 칩에서 고른 공식 카테고리 id
 * newCategories: "+추가"로 직접 입력한 카테고리 이름 (합쳐서 5개까지)
 * imageUrls: Cloudinary에 올린 이미지 URL (순서대로 표시)
 * draft: true면 임시 보관. 임시 보관한 질문을 false로 수정하면 등록된다. 등록된 질문은 다시 임시 보관할 수 없다.
 */
public record QuestionRequest(
        @NotBlank(message = "제목을 입력해 주세요.")
        @Size(max = 200, message = "제목은 200자 이하로 입력해 주세요.")
        String title,

        @NotBlank(message = "내용을 입력해 주세요.")
        @Size(max = 10000, message = "내용은 10,000자 이하로 입력해 주세요.")
        String content,

        List<Long> categoryIds,

        List<@NotBlank(message = "카테고리 이름을 입력해 주세요.")
             @Size(max = 30, message = "카테고리 이름은 30자 이하로 입력해 주세요.") String> newCategories,

        @Size(max = 10, message = "이미지는 10장까지 첨부할 수 있어요.")
        List<@Pattern(regexp = "^https://res\\.cloudinary\\.com/.+", message = "업로드한 이미지 주소만 사용할 수 있어요.")
             String> imageUrls,

        boolean anonymous,

        boolean draft
) {

    public List<Long> categoryIdsOrEmpty() {
        return categoryIds == null ? List.of() : categoryIds;
    }

    public List<String> newCategoriesOrEmpty() {
        return newCategories == null ? List.of() : newCategories;
    }

    public List<String> imageUrlsOrEmpty() {
        return imageUrls == null ? List.of() : imageUrls;
    }
}
