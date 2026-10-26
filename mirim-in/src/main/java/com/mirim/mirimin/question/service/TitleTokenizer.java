package com.mirim.mirimin.question.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 유사 질문 검색용으로 제목을 단어로 나눈다.
 * 한국어 형태소 분석기 없이 간단한 규칙만 쓴다: 끝에 붙은 조사·어미를 떼고, 흔한 말은 뺀다.
 * 예) "기숙사에서 통금은 몇 시인가요?" → [기숙사, 통금]
 */
final class TitleTokenizer {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_TOKENS = 6;

    // 긴 것부터 확인한다
    private static final List<String> SUFFIXES = List.of(
            "인가요", "하나요", "되나요", "있나요", "없나요", "할까요", "될까요", "까요", "나요", "인지", "해요",
            "에서는", "에서", "에게", "한테", "으로", "부터", "까지", "이랑", "랑",
            "은", "는", "이", "가", "을", "를", "의", "에", "도", "와", "과", "로", "요");

    private static final Set<String> STOPWORDS = Set.of(
            "질문", "궁금", "궁금해", "궁금합니다", "궁금해요", "어떻게", "어떤", "무엇", "뭐", "뭔가", "있는", "없는",
            "정도", "보통", "혹시", "그리고", "근데", "그런데", "진짜", "너무", "많이", "알려", "주세요", "인가", "건가",
            "하는", "되는", "몇", "시", "좀", "저도", "제가", "저는");

    private TitleTokenizer() {
    }

    static List<String> tokenize(String title) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String raw : title.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+")) {
            // "하나요", "되나요"처럼 단어 전체가 어미면 건너뛴다
            if (SUFFIXES.contains(raw)) {
                continue;
            }
            String word = stripSuffix(raw);
            if (word.length() >= MIN_LENGTH && !STOPWORDS.contains(word) && !STOPWORDS.contains(raw)) {
                tokens.add(word);
            }
            if (tokens.size() == MAX_TOKENS) {
                break;
            }
        }
        return List.copyOf(tokens);
    }

    private static String stripSuffix(String word) {
        for (String suffix : SUFFIXES) {
            if (word.endsWith(suffix) && word.length() - suffix.length() >= MIN_LENGTH) {
                return word.substring(0, word.length() - suffix.length());
            }
        }
        return word;
    }
}
