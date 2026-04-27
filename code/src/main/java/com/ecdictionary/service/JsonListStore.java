package com.ecdictionary.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class JsonListStore {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Path filePath;

    public JsonListStore(Path filePath) {
        this.filePath = filePath;
    }

    public List<String> load() {
        if (Files.notExists(filePath)) {
            return new ArrayList<>();
        }

        try {
            return objectMapper.readValue(filePath.toFile(), new TypeReference<>() { });
        } catch (IOException exception) {
            throw new UncheckedIOException("读取文件失败: " + filePath, exception);
        }
    }

    public void save(List<String> items) {
        try {
            Files.createDirectories(filePath.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), items);
        } catch (IOException exception) {
            throw new UncheckedIOException("保存文件失败: " + filePath, exception);
        }
    }
}
