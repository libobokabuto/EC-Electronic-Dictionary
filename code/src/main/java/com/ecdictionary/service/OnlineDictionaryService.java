package com.ecdictionary.service;

import com.ecdictionary.model.DictionaryEntry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public class OnlineDictionaryService implements RemoteLookupService {
    private static final String ENGLISH_DICTIONARY_API = "https://api.dictionaryapi.dev/api/v2/entries/en/";
    private static final String TRANSLATE_API = "https://api.mymemory.translated.net/get";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OnlineDictionaryService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), new ObjectMapper());
    }

    OnlineDictionaryService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public LookupResult lookup(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return LookupResult.emptyQuery();
        }

        try {
            return containsChinese(normalized) ? lookupChineseQuery(query.trim()) : lookupEnglishQuery(query.trim());
        } catch (IOException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return LookupResult.networkError(
                    "在线查询失败，请检查网络后重试。",
                    "本地词库未命中，且在线接口当前不可用或响应超时。"
            );
        }
    }

    private LookupResult lookupEnglishQuery(String query) throws IOException, InterruptedException {
        Optional<DictionaryEntry> dictionaryEntry = fetchEnglishDictionaryEntry(query);
        Optional<String> chineseTranslation = translate(query, "en", "zh-CN");

        if (dictionaryEntry.isPresent()) {
            DictionaryEntry entry = dictionaryEntry.get();
            if (chineseTranslation.isPresent()) {
                entry.setChineseKeyword(chineseTranslation.get());
                prependMeaning(entry, "中文释义：" + chineseTranslation.get());
                entry.setNote("在线词典补查结果，释义来自英语词典接口，并补充了在线翻译。");
                return LookupResult.success(entry, "在线词典 + 在线翻译", "本地词库未命中，已切换为在线结果。");
            }

            entry.setChineseKeyword("在线释义");
            entry.setNote("在线词典补查结果，未获取到中文翻译，已保留英文释义。");
            return LookupResult.success(entry, "在线词典", "本地词库未命中，已显示在线英文词典结果。");
        }

        if (chineseTranslation.isPresent()) {
            DictionaryEntry entry = buildTranslationOnlyEntry(query, chineseTranslation.get(), "在线翻译结果，未获取到更详细的词典释义。");
            return LookupResult.success(entry, "在线翻译", "本地词库未命中，已显示在线翻译结果。");
        }

        return LookupResult.notFound(query, "本地词库与在线接口都没有返回可用结果。");
    }

    private LookupResult lookupChineseQuery(String query) throws IOException, InterruptedException {
        Optional<String> englishTranslation = translate(query, "zh-CN", "en");
        if (englishTranslation.isEmpty()) {
            return LookupResult.notFound(query, "中文关键词在线翻译失败，无法继续补查英文词条。");
        }

        String englishWord = extractPrimaryEnglishCandidate(englishTranslation.get());
        Optional<DictionaryEntry> dictionaryEntry = fetchEnglishDictionaryEntry(englishWord);
        if (dictionaryEntry.isPresent()) {
            DictionaryEntry entry = dictionaryEntry.get();
            entry.setChineseKeyword(query);
            prependMeaning(entry, "中文关键词：" + query);
            entry.setNote("先由在线翻译得到英文候选词，再从在线词典补全释义。");
            return LookupResult.success(entry, "在线翻译 + 在线词典", "本地词库未命中，已根据中文关键词补查在线结果。");
        }

        DictionaryEntry entry = buildReverseTranslationEntry(englishWord, query);
        return LookupResult.success(entry, "在线翻译", "本地词库未命中，已显示在线翻译结果。");
    }

    private Optional<DictionaryEntry> fetchEnglishDictionaryEntry(String englishWord) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(ENGLISH_DICTIONARY_API + encode(englishWord)))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() == 404) {
            return Optional.empty();
        }
        if (response.statusCode() >= 400) {
            throw new IOException("dictionary api error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.isArray() || root.isEmpty()) {
            return Optional.empty();
        }

        JsonNode first = root.get(0);
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord(first.path("word").asText(englishWord));
        entry.setPhonetic(resolvePhonetic(first));
        entry.setPartOfSpeech(resolvePartOfSpeech(first));
        entry.setMeanings(resolveMeanings(first));
        entry.setExamples(resolveExamples(first));
        entry.setChineseKeyword("在线释义");
        entry.setNote("在线词典补查结果。");
        return Optional.of(entry);
    }

    private Optional<String> translate(String text, String sourceLanguage, String targetLanguage) throws IOException, InterruptedException {
        String uri = TRANSLATE_API
                + "?q=" + encode(text)
                + "&langpair=" + encode(sourceLanguage + "|" + targetLanguage);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .timeout(Duration.ofSeconds(8))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("translate api error: " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        String translated = root.path("responseData").path("translatedText").asText("").trim();
        if (translated.isEmpty() || translated.equalsIgnoreCase(text.trim())) {
            return Optional.empty();
        }
        return Optional.of(translated);
    }

    private String resolvePhonetic(JsonNode entryNode) {
        String phonetic = entryNode.path("phonetic").asText("").trim();
        if (!phonetic.isEmpty()) {
            return phonetic;
        }
        for (JsonNode phoneticsNode : entryNode.path("phonetics")) {
            String text = phoneticsNode.path("text").asText("").trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return "在线词典未提供音标";
    }

    private String resolvePartOfSpeech(JsonNode entryNode) {
        Set<String> parts = new LinkedHashSet<>();
        for (JsonNode meaningNode : entryNode.path("meanings")) {
            String part = meaningNode.path("partOfSpeech").asText("").trim();
            if (!part.isEmpty()) {
                parts.add(part);
            }
        }
        return parts.isEmpty() ? "online" : String.join(" / ", parts);
    }

    private List<String> resolveMeanings(JsonNode entryNode) {
        List<String> meanings = new ArrayList<>();
        for (JsonNode meaningNode : entryNode.path("meanings")) {
            String part = meaningNode.path("partOfSpeech").asText("").trim();
            int count = 0;
            for (JsonNode definitionNode : meaningNode.path("definitions")) {
                String definition = definitionNode.path("definition").asText("").trim();
                if (!definition.isEmpty()) {
                    meanings.add((part.isEmpty() ? "" : "[" + part + "] ") + definition);
                    count++;
                }
                if (count >= 2) {
                    break;
                }
            }
            if (meanings.size() >= 6) {
                break;
            }
        }
        if (meanings.isEmpty()) {
            meanings.add("在线接口返回了词条，但未提供详细释义。");
        }
        return meanings;
    }

    private List<String> resolveExamples(JsonNode entryNode) {
        List<String> examples = new ArrayList<>();
        for (JsonNode meaningNode : entryNode.path("meanings")) {
            for (JsonNode definitionNode : meaningNode.path("definitions")) {
                String example = definitionNode.path("example").asText("").trim();
                if (!example.isEmpty()) {
                    examples.add(example);
                }
                if (examples.size() >= 4) {
                    return examples;
                }
            }
        }
        if (examples.isEmpty()) {
            examples.add("在线接口暂未提供例句。");
        }
        return examples;
    }

    private DictionaryEntry buildTranslationOnlyEntry(String englishWord, String chineseTranslation, String note) {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord(englishWord);
        entry.setPhonetic("在线翻译结果");
        entry.setPartOfSpeech("translation");
        entry.setChineseKeyword(chineseTranslation);
        entry.setMeanings(List.of("中文释义：" + chineseTranslation));
        entry.setExamples(List.of("在线翻译模式未提供例句。"));
        entry.setNote(note);
        return entry;
    }

    private DictionaryEntry buildReverseTranslationEntry(String englishWord, String chineseQuery) {
        DictionaryEntry entry = new DictionaryEntry();
        entry.setWord(englishWord);
        entry.setPhonetic("在线翻译结果");
        entry.setPartOfSpeech("translation");
        entry.setChineseKeyword(chineseQuery);
        entry.setMeanings(List.of("根据中文关键词匹配到英文候选词：" + englishWord));
        entry.setExamples(List.of("在线翻译模式未提供例句。"));
        entry.setNote("未获取到更详细的在线英文词典释义，当前显示的是翻译候选结果。");
        return entry;
    }

    private void prependMeaning(DictionaryEntry entry, String line) {
        List<String> updated = new ArrayList<>();
        updated.add(line);
        updated.addAll(entry.getMeanings());
        entry.setMeanings(updated);
    }

    private String extractPrimaryEnglishCandidate(String translatedText) {
        String candidate = translatedText.trim();
        candidate = candidate.split("[,;/]")[0].trim();
        candidate = candidate.split("\\s+")[0].trim();
        return candidate.isEmpty() ? translatedText.trim() : candidate;
    }

    private boolean containsChinese(String value) {
        return value.codePoints().anyMatch(codePoint -> Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
