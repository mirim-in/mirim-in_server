package com.mirim.mirimin.user.service;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.global.exception.BusinessException;
import com.mirim.mirimin.global.exception.ErrorCode;
import com.mirim.mirimin.user.dto.VerificationRequestCreateRequest;
import com.mirim.mirimin.user.dto.VerificationRequestResponse;
import com.mirim.mirimin.user.entity.MemberType;
import com.mirim.mirimin.user.entity.MemberVerificationRequest;
import com.mirim.mirimin.user.entity.User;
import com.mirim.mirimin.user.entity.VerificationRequestStatus;
import com.mirim.mirimin.user.repository.MemberVerificationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 학교 이메일이 없는 사용자(주로 졸업생)의 미림인 승인 신청. 승인·반려는 관리자 기능에서 한다. */
@Service
@RequiredArgsConstructor
public class MemberVerificationService {

    private final MemberVerificationRequestRepository requestRepository;
    private final UserReader userReader;

    @Transactional
    public Long request(VerificationRequestCreateRequest request, AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        if (request.requestedType() == MemberType.GENERAL) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "졸업생, 교직원, 재학생 중에서 선택해 주세요.");
        }
        if (user.isVerifiedMember()) {
            throw new BusinessException(ErrorCode.ALREADY_VERIFIED);
        }
        if (requestRepository.existsByUserIdAndStatus(user.getId(), VerificationRequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.VERIFICATION_REQUEST_PENDING);
        }
        String message = request.message() == null || request.message().isBlank() ? null : request.message().trim();
        return requestRepository.save(new MemberVerificationRequest(user, request.requestedType(), message)).getId();
    }

    @Transactional(readOnly = true)
    public List<VerificationRequestResponse> getMyRequests(AuthUser authUser) {
        User user = userReader.getActiveUser(authUser);
        return requestRepository.findByUserIdOrderByIdDesc(user.getId()).stream()
                .map(VerificationRequestResponse::of)
                .toList();
    }
}
