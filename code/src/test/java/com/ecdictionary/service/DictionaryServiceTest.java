package com.ecdictionary.service;

import com.ecdictionary.model.DictionaryEntry;
import com.ecdictionary.repository.DictionaryRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DictionaryServiceTest {
    @Test
    void shouldFindEnglishWordFromLocalDictionary() {
        DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository(), query -> {
            throw new AssertionError("Local hit should not call remote service");
        });

        LookupResult result = dictionaryService.lookup("apple");
        assertTrue(result.isSuccess());
        assertEquals("本地词库", result.getSourceLabel());
        assertEquals("苹果", result.getEntry().getChineseKeyword());
    }

    @Test
    void shouldFindChineseKeywordFromLocalDictionary() {
        DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository(), query -> {
            throw new AssertionError("Local hit should not call remote service");
        });

        LookupResult result = dictionaryService.lookup("学习");
        assertTrue(result.isSuccess());
        assertEquals("learn", result.getEntry().getWord());
    }

    @Test
    void shouldSuggestFuzzyMatches() {
        DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository(), query -> LookupResult.notFound(query, ""));

        var result = dictionaryService.suggest("eff", 5);
        assertFalse(result.isEmpty());
        assertEquals("efficient", result.getFirst().getWord());
    }

    @Test
    void shouldUseRemoteFallbackForUnknownWord() {
        DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository(), query ->
                LookupResult.success(remoteEntry(), "在线翻译", "已使用在线结果。"));

        LookupResult result = dictionaryService.lookup("unlisted-word");
        assertTrue(result.isSuccess());
        assertEquals("online", result.getEntry().getWord());
        assertEquals("在线翻译", result.getSourceLabel());
    }

    @Test
    void shouldReturnEmptyStateForBlankQuery() {
        DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository(), query -> LookupResult.notFound(query, ""));

        LookupResult result = dictionaryService.lookup("   ");
        assertEquals(LookupStatus.EMPTY_QUERY, result.getStatus());
    }

    private DictionaryEntry remoteEntry() {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord("online");
        entry.setPhonetic("remote");
        entry.setPartOfSpeech("n.");
        entry.setMeanings(List.of("在线结果"));
        entry.setExamples(List.of("This is a remote lookup example."));
        entry.setChineseKeyword("在线");
        entry.setNote("测试用远程结果");
        return entry;
    }
}
