package com.ecdictionary.service;

import com.ecdictionary.model.DictionaryEntry;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class FavoriteService {
    private final JsonListStore store;
    private final ObservableList<String> favoriteWords = FXCollections.observableArrayList();

    public FavoriteService(Path dataDirectory) {
        this.store = new JsonListStore(dataDirectory.resolve("favorites.json"));
        favoriteWords.setAll(store.load());
    }

    public ObservableList<String> getFavoriteWords() {
        return favoriteWords;
    }

    public boolean isFavorite(DictionaryEntry entry) {
        return favoriteWords.contains(entry.getWord());
    }

    public boolean add(DictionaryEntry entry) {
        if (entry == null || favoriteWords.contains(entry.getWord())) {
            return false;
        }
        favoriteWords.add(0, entry.getWord());
        persist();
        return true;
    }

    public boolean remove(DictionaryEntry entry) {
        if (entry == null) {
            return false;
        }
        boolean removed = favoriteWords.remove(entry.getWord());
        if (removed) {
            persist();
        }
        return removed;
    }

    public void cleanupMissingWords(Set<String> validWords) {
        LinkedHashSet<String> filtered = new LinkedHashSet<>(favoriteWords);
        filtered.removeIf(word -> !validWords.contains(word));
        if (filtered.size() != favoriteWords.size()) {
            favoriteWords.setAll(filtered);
            persist();
        }
    }

    private void persist() {
        store.save(new ArrayList<>(favoriteWords));
    }
}
