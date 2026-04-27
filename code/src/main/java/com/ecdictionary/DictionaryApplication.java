package com.ecdictionary;

import com.ecdictionary.model.DictionaryEntry;
import com.ecdictionary.repository.DictionaryRepository;
import com.ecdictionary.service.DictionaryService;
import com.ecdictionary.service.FavoriteService;
import com.ecdictionary.service.HistoryService;
import com.ecdictionary.service.LookupResult;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class DictionaryApplication extends javafx.application.Application {
    private final DictionaryService dictionaryService = new DictionaryService(new DictionaryRepository());
    private final Path dataDirectory = Path.of(System.getProperty("user.home"), "EnglishChineseDictionary", "app-data");
    private final HistoryService historyService = new HistoryService(dataDirectory);
    private final FavoriteService favoriteService = new FavoriteService(dataDirectory);

    private final TextField searchField = new TextField();
    private final ListView<DictionaryEntry> suggestionList = new ListView<>();
    private final ListView<String> historyList = new ListView<>();
    private final ListView<String> favoriteList = new ListView<>();
    private final Label wordLabel = new Label("准备开始检索");
    private final Label phoneticLabel = new Label("输入英文或中文关键词，试试例如 apple、学习、勇气");
    private final Label partOfSpeechLabel = new Label("双向词典");
    private final Label sourceLabel = new Label("来源：本地词库");
    private final Label meaningsLabel = new Label("这里会显示词性、释义、例句和补充说明。");
    private final Label examplesLabel = new Label("支持本地词库优先查询，查不到时自动联网补查。");
    private final Label noteLabel = new Label("第二阶段已支持本地 + 在线双模式查询。");
    private final Label statusLabel = new Label("词库已加载，随时可以开始。");
    private final Button favoriteButton = new Button("收藏当前词条");

    private DictionaryEntry currentEntry;

    @Override
    public void start(Stage stage) {
        configureControls();

        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(buildHeader());
        root.setLeft(buildSidebar("最近查询", historyList, "点击可重新查询最近检索过的内容。"));
        root.setCenter(buildResultPane());
        root.setRight(buildSidebar("我的收藏", favoriteList, "收藏常用词条，二次演示更顺手。"));
        root.setBottom(buildStatusBar());
        BorderPane.setMargin(root.getLeft(), new Insets(0, 8, 16, 16));
        BorderPane.setMargin(root.getCenter(), new Insets(0, 8, 16, 8));
        BorderPane.setMargin(root.getRight(), new Insets(0, 16, 16, 8));

        Scene scene = new Scene(root, 1360, 860);
        scene.getStylesheets().add(getClass().getResource("/styles/app.css").toExternalForm());

        stage.setTitle("英汉电子词典软件 - 第二阶段增强版");
        stage.setScene(scene);
        stage.show();
    }

    private void configureControls() {
        historyList.setItems(historyService.getHistoryItems());
        favoriteList.setItems(favoriteService.getFavoriteWords());
        suggestionList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DictionaryEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getWord() + "  /  " + item.getChineseKeyword());
            }
        });
        historyList.setPlaceholder(new Label("暂无历史记录"));
        favoriteList.setPlaceholder(new Label("暂无收藏词条"));
        suggestionList.setPlaceholder(new Label("没有本地联想结果"));
        suggestionList.setPrefHeight(240);
        suggestionList.setVisible(false);
        suggestionList.setManaged(false);

        searchField.setPromptText("输入英文单词或中文关键词");
        searchField.textProperty().addListener((observable, oldValue, newValue) -> refreshSuggestions(newValue));
        searchField.setOnAction(event -> executeSearch(searchField.getText()));
        searchField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DOWN && !suggestionList.getItems().isEmpty()) {
                suggestionList.requestFocus();
                suggestionList.getSelectionModel().selectFirst();
            }
        });

        suggestionList.setOnMouseClicked(event -> {
            DictionaryEntry selected = suggestionList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                executeSearch(selected.getWord());
            }
        });
        suggestionList.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                DictionaryEntry selected = suggestionList.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    executeSearch(selected.getWord());
                }
            }
        });

        historyList.setOnMouseClicked(event -> {
            String selected = historyList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                executeSearch(selected);
            }
        });
        favoriteList.setOnMouseClicked(event -> {
            String selected = favoriteList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                executeSearch(selected);
            }
        });
        favoriteList.getItems().addListener((ListChangeListener<String>) change -> updateFavoriteButton());

        favoriteButton.setOnAction(event -> toggleFavorite());
        updateFavoriteButton();
    }

    private VBox buildHeader() {
        Label eyebrow = new Label("Stage 2 Enhanced JavaFX Dictionary");
        eyebrow.getStyleClass().add("eyebrow");

        Label title = new Label("英汉电子词典软件");
        title.getStyleClass().add("hero-title");

        Button searchButton = new Button("立即查询");
        searchButton.setOnAction(event -> executeSearch(searchField.getText()));

        Button clearButton = new Button("清空");
        clearButton.getStyleClass().add("secondary-button");
        clearButton.setOnAction(event -> {
            searchField.clear();
            searchField.requestFocus();
            showStatus("已清空输入，可以继续检索。");
        });

        HBox inputRow = new HBox(12, searchField, searchButton, clearButton);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        VBox heroPanel = new VBox(16, eyebrow, title, inputRow, suggestionList);
        heroPanel.getStyleClass().add("hero-panel");
        heroPanel.setPadding(new Insets(24, 28, 28, 28));

        VBox wrapper = new VBox(heroPanel);
        wrapper.setPadding(new Insets(16, 16, 12, 16));
        return wrapper;
    }

    private VBox buildSidebar(String titleText, ListView<String> listView, String helperText) {
        Label title = new Label(titleText);
        title.getStyleClass().add("panel-title");
        Label helper = new Label(helperText);
        helper.getStyleClass().add("panel-helper");

        VBox card = new VBox(12, title, helper, listView);
        card.getStyleClass().add("side-card");
        VBox.setVgrow(listView, Priority.ALWAYS);
        return card;
    }

    private ScrollPane buildResultPane() {
        wordLabel.getStyleClass().add("result-word");
        phoneticLabel.getStyleClass().add("result-phonetic");
        partOfSpeechLabel.getStyleClass().add("tag");
        sourceLabel.getStyleClass().add("tag");

        HBox metadataRow = new HBox(10, partOfSpeechLabel, sourceLabel);
        metadataRow.setAlignment(Pos.CENTER_LEFT);

        VBox resultCard = new VBox(
                18,
                wordLabel,
                phoneticLabel,
                metadataRow,
                section("核心释义", meaningsLabel),
                section("例句参考", examplesLabel),
                section("词条备注", noteLabel),
                buildActionRow()
        );
        resultCard.getStyleClass().add("result-card");
        resultCard.setPadding(new Insets(24));

        ScrollPane scrollPane = new ScrollPane(resultCard);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.getStyleClass().add("result-scroll");
        return scrollPane;
    }

    private VBox section(String titleText, Label contentLabel) {
        Label title = new Label(titleText);
        title.getStyleClass().add("section-title");
        contentLabel.setWrapText(true);
        contentLabel.getStyleClass().add("content-label");

        VBox box = new VBox(8, title, contentLabel);
        box.getStyleClass().add("section-card");
        return box;
    }

    private HBox buildActionRow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, spacer, favoriteButton);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private StackPane buildStatusBar() {
        statusLabel.getStyleClass().add("status-label");
        StackPane container = new StackPane(statusLabel);
        container.getStyleClass().add("status-bar");
        StackPane.setAlignment(statusLabel, Pos.CENTER_LEFT);
        container.setPadding(new Insets(12, 20, 14, 20));
        return container;
    }

    private void refreshSuggestions(String query) {
        List<DictionaryEntry> matches = dictionaryService.suggest(query, 8);
        suggestionList.getItems().setAll(matches);
        boolean visible = !matches.isEmpty() && !normalize(query).isEmpty();
        suggestionList.setVisible(visible);
        suggestionList.setManaged(visible);
    }

    private void executeSearch(String query) {
        LookupResult result = dictionaryService.lookup(query);
        if (result.isSuccess()) {
            currentEntry = result.getEntry();
            renderResult(result);
            historyService.record(query.trim());
            searchField.setText(currentEntry.getWord());
            searchField.positionCaret(currentEntry.getWord().length());
            suggestionList.setVisible(false);
            suggestionList.setManaged(false);
            showStatus(result.getStatusMessage());
            updateFavoriteButton();
            return;
        }

        currentEntry = null;
        renderFailure(result, query);
        updateFavoriteButton();
    }

    private void renderResult(LookupResult result) {
        DictionaryEntry entry = result.getEntry();
        wordLabel.setText(entry.getWord());
        phoneticLabel.setText(entry.getPhonetic());
        partOfSpeechLabel.setText(entry.getPartOfSpeech() + "  /  " + entry.getChineseKeyword());
        sourceLabel.setText("来源：" + result.getSourceLabel());
        meaningsLabel.setText(joinLines(entry.getMeanings()));
        examplesLabel.setText(joinLines(entry.getExamples()));
        noteLabel.setText(entry.getNote());
    }

    private void renderFailure(LookupResult result, String query) {
        String safeQuery = query == null ? "" : query.trim();
        wordLabel.setText("未找到匹配结果");
        phoneticLabel.setText(result.getStatusMessage());
        partOfSpeechLabel.setText(safeQuery.isEmpty() ? "请输入查询内容" : "关键词：" + safeQuery);
        sourceLabel.setText("来源：" + result.getSourceLabel());
        meaningsLabel.setText(result.getDetailMessage());
        examplesLabel.setText("你可以尝试输入更短的英文前缀、常见中文关键词，或从左上方联想列表中选择。");
        noteLabel.setText("如果本地词库未命中，程序会自动尝试在线补查，并在此处说明当前状态。");
        showStatus(result.getStatusMessage());
    }

    private String joinLines(List<String> items) {
        return items.stream()
                .map(item -> "• " + item)
                .collect(Collectors.joining("\n"));
    }

    private void toggleFavorite() {
        if (currentEntry == null) {
            showStatus("当前没有可收藏的词条，请先完成一次有效查询。");
            return;
        }

        if (favoriteService.isFavorite(currentEntry)) {
            favoriteService.remove(currentEntry);
            showStatus("已取消收藏：" + currentEntry.getWord());
        } else if (favoriteService.add(currentEntry)) {
            showStatus("收藏成功：" + currentEntry.getWord());
        } else {
            showStatus("该词条已在收藏列表中。");
        }
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        if (currentEntry == null) {
            favoriteButton.setText("收藏当前词条");
            favoriteButton.setDisable(true);
            return;
        }

        boolean favorite = favoriteService.isFavorite(currentEntry);
        favoriteButton.setText(favorite ? "取消收藏" : "收藏当前词条");
        favoriteButton.setDisable(false);
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
