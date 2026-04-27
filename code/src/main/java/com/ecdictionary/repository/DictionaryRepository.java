package com.ecdictionary.repository;

import com.ecdictionary.model.DictionaryEntry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class DictionaryRepository {
    private final List<DictionaryEntry> entries;

    public DictionaryRepository() {
        this.entries = loadEntries();
    }

    public List<DictionaryEntry> getEntries() {
        return Collections.unmodifiableList(entries);
    }

    private List<DictionaryEntry> loadEntries() {
        ObjectMapper objectMapper = new ObjectMapper();
        try (InputStream inputStream = getClass().getResourceAsStream("/data/dictionary.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("词库资源未找到：/data/dictionary.json");
            }
            List<DictionaryEntry> loaded = objectMapper.readValue(inputStream, new TypeReference<>() { });
            loaded.removeIf(Objects::isNull);
            loaded.sort((left, right) -> left.getWord().toLowerCase(Locale.ROOT)
                    .compareTo(right.getWord().toLowerCase(Locale.ROOT)));
            return loaded;
        } catch (IOException exception) {
            throw new UncheckedIOException("词库读取失败", exception);
        }
    }
}
