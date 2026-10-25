package com.mirim.mirimin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 보낸 목록으로 통째로 바뀐다. 대소문자만 다른 중복은 처음 것만 남긴다. */
public record TechStacksRequest(
        @Size(max = 20, message = "기술 스택은 20개까지 등록할 수 있어요.")
        List<@NotBlank(message = "기술 스택 이름을 입력해 주세요.")
             @Size(max = 50, message = "기술 스택 이름은 50자 이하로 입력해 주세요.") String> names
) {

    public List<String> namesOrEmpty() {
        return names == null ? List.of() : names;
    }
}
