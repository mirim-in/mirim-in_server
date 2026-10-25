package com.mirim.mirimin.user.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 공식 카테고리 id 목록. 보낸 목록으로 통째로 바뀐다. (빈 목록이면 모두 해제)
 * answerableIds(답변 가능 분야)는 학교 구성원 인증을 받은 사용자만 설정할 수 있다.
 */
public record UserCategoriesRequest(
        @Size(max = 10, message = "관심 분야는 10개까지 선택할 수 있어요.")
        List<Long> interestIds,

        @Size(max = 10, message = "답변 가능 분야는 10개까지 선택할 수 있어요.")
        List<Long> answerableIds
) {

    public List<Long> interestIdsOrEmpty() {
        return interestIds == null ? List.of() : interestIds;
    }

    public List<Long> answerableIdsOrEmpty() {
        return answerableIds == null ? List.of() : answerableIds;
    }
}
