package com.mirim.mirimin.question.dto;

import org.springframework.data.domain.Sort;

public enum QuestionSort {
    LATEST(Sort.by(Sort.Order.desc("publishedAt"), Sort.Order.desc("id"))),
    VIEWS(Sort.by(Sort.Order.desc("viewCount"), Sort.Order.desc("id"))),
    ANSWERS(Sort.by(Sort.Order.desc("answerCount"), Sort.Order.desc("id")));

    private final Sort sort;

    QuestionSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        return sort;
    }
}
