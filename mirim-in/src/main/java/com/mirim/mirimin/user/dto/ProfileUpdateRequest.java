package com.mirim.mirimin.user.dto;

import com.mirim.mirimin.user.entity.Department;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 재학생의 학과·학년은 이메일에서 정해지므로 department를 보내도 무시된다.
 * 학년은 누구도 직접 바꿀 수 없다.
 */
public record ProfileUpdateRequest(
        @NotBlank(message = "이름을 입력해 주세요.")
        @Size(max = 30, message = "이름은 30자 이하로 입력해 주세요.")
        String name,

        @Min(value = 1, message = "반을 올바르게 선택해 주세요.")
        @Max(value = 20, message = "반을 올바르게 선택해 주세요.")
        Integer classNo,

        @Size(max = 500, message = "자기소개는 500자 이하로 입력해 주세요.")
        String bio,

        Department department
) {
}
