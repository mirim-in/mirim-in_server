package com.mirim.mirimin.auth.service;

import com.mirim.mirimin.global.config.AppProperties;
import com.mirim.mirimin.user.entity.Department;
import com.mirim.mirimin.user.entity.MemberType;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이메일로 회원 유형을 판별한다.
 * - 학교 도메인 + "영문 1자 + 숫자 4자"(s2501) → 재학생. 첫 글자 학과, 다음 두 자리 입학년도
 * - 학교 도메인 + 그 외 → 교직원
 * - 그 외 도메인 → 일반
 * 학교 도메인 소유 확인(인증 코드, Google 로그인)이 끝난 이메일에만 사용해야 한다.
 */
@Component
public class SchoolEmailClassifier {

    private static final Pattern STUDENT_LOCAL_PART = Pattern.compile("^([a-zA-Z])(\\d{2})(\\d{2})$");

    private final String schoolDomain;

    public SchoolEmailClassifier(AppProperties appProperties) {
        this.schoolDomain = appProperties.auth().schoolEmailDomain();
    }

    public Classification classify(String email) {
        int at = email.lastIndexOf('@');
        String localPart = email.substring(0, at);
        String domain = email.substring(at + 1);

        if (!domain.equalsIgnoreCase(schoolDomain)) {
            return new Classification(MemberType.GENERAL, null, null);
        }
        Matcher m = STUDENT_LOCAL_PART.matcher(localPart);
        if (m.matches()) {
            // 아직 매핑이 없는 학과 코드여도 재학생으로 인정하고 학과만 비워 둔다
            Department department = Department.fromCode(m.group(1).charAt(0)).orElse(null);
            int admissionYear = 2000 + Integer.parseInt(m.group(2));
            return new Classification(MemberType.STUDENT, department, admissionYear);
        }
        return new Classification(MemberType.TEACHER, null, null);
    }

    public record Classification(MemberType memberType, Department department, Integer admissionYear) {
    }
}
