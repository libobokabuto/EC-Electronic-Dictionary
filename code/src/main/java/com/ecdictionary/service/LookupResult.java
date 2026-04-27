package com.ecdictionary.service;

import com.ecdictionary.model.DictionaryEntry;

public class LookupResult {
    private final LookupStatus status;
    private final DictionaryEntry entry;
    private final String sourceLabel;
    private final String statusMessage;
    private final String detailMessage;

    private LookupResult(
            LookupStatus status,
            DictionaryEntry entry,
            String sourceLabel,
            String statusMessage,
            String detailMessage
    ) {
        this.status = status;
        this.entry = entry;
        this.sourceLabel = sourceLabel;
        this.statusMessage = statusMessage;
        this.detailMessage = detailMessage;
    }

    public static LookupResult success(DictionaryEntry entry, String sourceLabel, String statusMessage) {
        return new LookupResult(LookupStatus.SUCCESS, entry, sourceLabel, statusMessage, "");
    }

    public static LookupResult emptyQuery() {
        return new LookupResult(
                LookupStatus.EMPTY_QUERY,
                null,
                "未查询",
                "请输入英文单词或中文关键词。",
                "支持本地词库优先查询，查不到时会尝试在线补查。"
        );
    }

    public static LookupResult notFound(String query, String detailMessage) {
        return new LookupResult(
                LookupStatus.NOT_FOUND,
                null,
                "未命中",
                "没有检索到与“" + query + "”相关的结果。",
                detailMessage
        );
    }

    public static LookupResult networkError(String statusMessage, String detailMessage) {
        return new LookupResult(LookupStatus.NETWORK_ERROR, null, "在线查询失败", statusMessage, detailMessage);
    }

    public LookupStatus getStatus() {
        return status;
    }

    public DictionaryEntry getEntry() {
        return entry;
    }

    public String getSourceLabel() {
        return sourceLabel;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getDetailMessage() {
        return detailMessage;
    }

    public boolean isSuccess() {
        return status == LookupStatus.SUCCESS && entry != null;
    }
}
