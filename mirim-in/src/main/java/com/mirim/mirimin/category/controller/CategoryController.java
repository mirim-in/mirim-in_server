package com.mirim.mirimin.category.controller;

import com.mirim.mirimin.category.dto.CategoryResponse;
import com.mirim.mirimin.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "카테고리")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @Operation(summary = "공식 카테고리 목록", description = "질문 목록 필터 칩, 질문 작성 칩, 답변 가능 분야 선택지에 쓴다.")
    @GetMapping
    public List<CategoryResponse> getOfficialCategories() {
        return categoryService.getOfficialCategories().stream().map(CategoryResponse::of).toList();
    }
}
