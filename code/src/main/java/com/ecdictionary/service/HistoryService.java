package com.ecdictionary.service;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class HistoryService {
    private static final int MAX_ITEMS = 12;
    private final JsonListStore store;
    private final ObservableList<String> historyItems = FXCollections.observableArrayList();

    public HistoryService(Path dataDirectory) {
        this.store = new JsonListStore(dataDirectory.resolve("history.json"));
        historyItems.setAll(store.load());
    }

    public ObservableList<String> getHistoryItems() {
        return historyItems;
    }

    public void record(String query) {
        String normalized = query == null ? "" : query.trim();
        if (normalized.isEmpty()) {
            return;
        }

        LinkedHashSet<String> merged = new LinkedHashSet<>();
        merged.add(normalized);
        merged.addAll(historyItems);
        List<String> latest = new ArrayList<>(merged).subList(0, Math.min(MAX_ITEMS, merged.size()));
        historyItems.setAll(latest);
        store.save(new ArrayList<>(historyItems));
    }
}
