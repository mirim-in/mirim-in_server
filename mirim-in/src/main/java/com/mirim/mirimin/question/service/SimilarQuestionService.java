package com.mirim.mirimin.question.service;

import com.mirim.mirimin.question.dto.QuestionSummaryResponse;
import com.mirim.mirimin.question.entity.Question;
import com.mirim.mirimin.question.entity.QuestionCategory;
import com.mirim.mirimin.question.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 유사 질문 추천 (QD-007). 질문 작성 중 제목·카테고리가 비슷한 기존 질문을 보여줘 중복 질문을 줄인다.
 * 점수: 제목에 겹치는 단어 1개당 2점, 겹치는 카테고리 1개당 1점. 같으면 채택 완료 → 최신 순
 */
@Service
@RequiredArgsConstructor
public class SimilarQuestionService {

    private static final int TITLE_WEIGHT = 2;
    private static final int CATEGORY_WEIGHT = 1;
    private static final Pageable CANDIDATES_PER_QUERY = PageRequest.of(0, 30);

    private final QuestionRepository questionRepository;

    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> findSimilar(String title, List<Long> categoryIds, Long excludeId, int size) {
        List<String> tokens = TitleTokenizer.tokenize(title);
        Set<Long> categories = Set.copyOf(categoryIds);

        Map<Long, Question> candidates = new LinkedHashMap<>();
        tokens.forEach(token -> questionRepository.findCandidatesByTitle(token, CANDIDATES_PER_QUERY)
                .forEach(q -> candidates.putIfAbsent(q.getId(), q)));
        if (!categories.isEmpty()) {
            questionRepository.findCandidatesByCategories(categories, CANDIDATES_PER_QUERY)
                    .forEach(q -> candidates.putIfAbsent(q.getId(), q));
        }
        if (excludeId != null) {
            candidates.remove(excludeId);
        }

        Map<Question, Integer> scores = candidates.values().stream()
                .collect(Collectors.toMap(q -> q, q -> score(q, tokens, categories)));
        return scores.entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .sorted(Comparator.<Map.Entry<Question, Integer>>comparingInt(Map.Entry::getValue).reversed()
                        .thenComparing(e -> !e.getKey().isResolved())
                        .thenComparing(e -> e.getKey().getId(), Comparator.reverseOrder()))
                .limit(size)
                .map(e -> QuestionSummaryResponse.of(e.getKey()))
                .toList();
    }

    private int score(Question question, List<String> tokens, Set<Long> categoryIds) {
        String title = question.getTitle().toLowerCase(Locale.ROOT);
        long titleMatches = tokens.stream().filter(title::contains).count();
        long categoryMatches = question.getCategories().stream()
                .map(QuestionCategory::getCategory)
                .filter(c -> !c.isDeleted() && categoryIds.contains(c.getId()))
                .count();
        return (int) (titleMatches * TITLE_WEIGHT + categoryMatches * CATEGORY_WEIGHT);
    }
}
