package com.mirim.mirimin.report.controller;

import com.mirim.mirimin.auth.jwt.AuthUser;
import com.mirim.mirimin.report.dto.ReportRequest;
import com.mirim.mirimin.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "신고")
@RestController
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고하기",
            description = "질문·답변·댓글. 본인 글은 불가, 같은 글은 한 번만. 기타(ETC) 사유는 detail 필요. 응답은 신고 id")
    @PostMapping("/api/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Long report(@Valid @RequestBody ReportRequest request, @AuthenticationPrincipal AuthUser authUser) {
        return reportService.report(request, authUser);
    }
}
