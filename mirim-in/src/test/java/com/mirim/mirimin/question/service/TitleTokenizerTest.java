package com.mirim.mirimin.question.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TitleTokenizerTest {

    @Test
    void 조사를_떼고_흔한_말은_뺀다() {
        assertThat(TitleTokenizer.tokenize("기숙사에서 통금은 몇 시인가요?"))
                .contains("기숙사", "통금")
                .doesNotContain("몇", "기숙사에서", "통금은");
        assertThat(TitleTokenizer.tokenize("면접 준비는 어떻게 하나요"))
                .containsExactly("면접", "준비");
    }

    @Test
    void 영문은_소문자로_바꾸고_중복을_없앤다() {
        assertThat(TitleTokenizer.tokenize("Java랑 java 중에 React"))
                .containsExactly("java", "중에", "react");
    }

    @Test
    void 두_글자보다_짧은_단어는_뺀다() {
        assertThat(TitleTokenizer.tokenize("a b c 시")).isEmpty();
    }
}
