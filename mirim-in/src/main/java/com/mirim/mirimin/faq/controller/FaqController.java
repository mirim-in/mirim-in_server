package com.mirim.mirimin.faq.controller;

import com.mirim.mirimin.faq.dto.FaqResponse;
import com.mirim.mirimin.faq.service.FaqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "FAQ")
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @Operation(summary = "FAQ 목록", description = "categoryId가 없으면 전체. 노출 순서대로")
    @GetMapping
    public List<FaqResponse> list(@RequestParam(required = false) Long categoryId) {
        return faqService.getFaqs(categoryId);
    }
}
