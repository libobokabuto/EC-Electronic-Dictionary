package com.ecdictionary.model;

import java.util.List;

public class DictionaryEntry {
    private String word;
    private String phonetic;
    private String partOfSpeech;
    private List<String> meanings;
    private List<String> examples;
    private String chineseKeyword;
    private String note;

    public String getWord() {
        return word;
    }

    public void setWord(String word) {
        this.word = word;
    }

    public String getPhonetic() {
        return phonetic;
    }

    public void setPhonetic(String phonetic) {
        this.phonetic = phonetic;
    }

    public String getPartOfSpeech() {
        return partOfSpeech;
    }

    public void setPartOfSpeech(String partOfSpeech) {
        this.partOfSpeech = partOfSpeech;
    }

    public List<String> getMeanings() {
        return meanings;
    }

    public void setMeanings(List<String> meanings) {
        this.meanings = meanings;
    }

    public List<String> getExamples() {
        return examples;
    }

    public void setExamples(List<String> examples) {
        this.examples = examples;
    }

    public String getChineseKeyword() {
        return chineseKeyword;
    }

    public void setChineseKeyword(String chineseKeyword) {
        this.chineseKeyword = chineseKeyword;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
