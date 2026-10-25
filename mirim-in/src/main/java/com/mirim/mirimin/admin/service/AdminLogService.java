package com.mirim.mirimin.admin.service;

import com.mirim.mirimin.admin.entity.AdminAction;
import com.mirim.mirimin.admin.entity.AdminLog;
import com.mirim.mirimin.admin.repository.AdminLogRepository;
import com.mirim.mirimin.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

/** 관리자 조치 기록 (AD-018). 조회 API는 아직 없다. */
@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    @Transactional
    public void record(Long adminId, AdminAction action, String targetType, Long targetId, String detail) {
        adminLogRepository.save(AdminLog.builder()
                .admin(userRepository.getReferenceById(adminId))
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .detail(detail)
                .now(LocalDateTime.now(clock))
                .build());
    }
}
