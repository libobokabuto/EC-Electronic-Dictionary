package com.ecdictionary.service;

import com.ecdictionary.model.DictionaryEntry;
import com.ecdictionary.repository.DictionaryRepository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DictionaryService {
    private final List<DictionaryEntry> entries;
    private final RemoteLookupService remoteLookupService;

    public DictionaryService(DictionaryRepository repository) {
        this(repository, new OnlineDictionaryService());
    }

    public DictionaryService(DictionaryRepository repository, RemoteLookupService remoteLookupService) {
        this.entries = repository.getEntries();
        this.remoteLookupService = remoteLookupService;
    }

    public LookupResult lookup(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return LookupResult.emptyQuery();
        }

        Optional<DictionaryEntry> localMatch = findBestMatch(query);
        if (localMatch.isPresent()) {
            return LookupResult.success(localMatch.get(), "本地词库", "查询成功：已命中本地词库。");
        }

        return remoteLookupService.lookup(query);
    }

    public Optional<DictionaryEntry> findBestMatch(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        return entries.stream()
                .filter(entry -> matchesExactly(entry, normalized))
                .findFirst()
                .or(() -> entries.stream()
                        .filter(entry -> matchesFuzzy(entry, normalized))
                        .min(Comparator.comparingInt(entry -> score(entry, normalized))));
    }

    public List<DictionaryEntry> suggest(String query, int limit) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return entries.stream().limit(limit).toList();
        }

        List<DictionaryEntry> exactPrefix = entries.stream()
                .filter(entry -> startsWith(entry, normalized))
                .limit(limit)
                .toList();
        if (exactPrefix.size() >= limit) {
            return exactPrefix;
        }

        List<DictionaryEntry> matches = new ArrayList<>(exactPrefix);
        entries.stream()
                .filter(entry -> !matches.contains(entry))
                .filter(entry -> matchesFuzzy(entry, normalized))
                .sorted(Comparator.comparingInt(entry -> score(entry, normalized)))
                .limit(Math.max(0, limit - matches.size()))
                .forEach(matches::add);
        return matches;
    }

    public Optional<DictionaryEntry> findByWord(String word) {
        String normalized = normalize(word);
        return entries.stream()
                .filter(entry -> normalize(entry.getWord()).equals(normalized))
                .findFirst();
    }

    private boolean matchesExactly(DictionaryEntry entry, String normalizedQuery) {
        return normalize(entry.getWord()).equals(normalizedQuery)
                || normalize(entry.getChineseKeyword()).equals(normalizedQuery);
    }

    private boolean startsWith(DictionaryEntry entry, String normalizedQuery) {
        return normalize(entry.getWord()).startsWith(normalizedQuery)
                || normalize(entry.getChineseKeyword()).startsWith(normalizedQuery);
    }

    private boolean matchesFuzzy(DictionaryEntry entry, String normalizedQuery) {
        return normalize(entry.getWord()).contains(normalizedQuery)
                || normalize(entry.getChineseKeyword()).contains(normalizedQuery)
                || entry.getMeanings().stream().map(this::normalize).anyMatch(item -> item.contains(normalizedQuery))
                || entry.getExamples().stream().map(this::normalize).anyMatch(item -> item.contains(normalizedQuery))
                || normalize(entry.getNote()).contains(normalizedQuery);
    }

    private int score(DictionaryEntry entry, String normalizedQuery) {
        String word = normalize(entry.getWord());
        String chinese = normalize(entry.getChineseKeyword());
        if (word.startsWith(normalizedQuery) || chinese.startsWith(normalizedQuery)) {
            return 0;
        }
        if (word.contains(normalizedQuery) || chinese.contains(normalizedQuery)) {
            return 1;
        }
        return 2;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
