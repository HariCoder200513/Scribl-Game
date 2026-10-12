package com.scribl.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class WordProviderTest {

    private final WordProvider provider = new WordProvider();

    @Test
    void pickReturnsAWordFromTheDictionary() {
        for (int i = 0; i < 100; i++) {
            assertThat(provider.pick(Set.of())).isIn(WordProvider.WORDS);
        }
    }

    @Test
    void pickNeverReturnsAnExcludedWord() {
        Set<String> excluded = new HashSet<>(WordProvider.WORDS);
        excluded.remove("apple");
        for (int i = 0; i < 100; i++) {
            assertThat(provider.pick(excluded)).isEqualTo("apple");
        }
    }

    @Test
    void pickFallsBackToFullDictionaryWhenEverythingIsExcluded() {
        Set<String> all = new HashSet<>(WordProvider.WORDS);
        for (int i = 0; i < 100; i++) {
            assertThat(provider.pick(all)).isIn(WordProvider.WORDS);
        }
    }

    @Test
    void dictionaryHasNoDuplicatesAndIsAllLowercase() {
        assertThat(WordProvider.WORDS).doesNotHaveDuplicates();
        assertThat(WordProvider.WORDS).allMatch(w -> w.equals(w.toLowerCase()));
    }
}