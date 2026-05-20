package dev.ritterackr.bitterlog.controller;

import com.sun.tools.javac.Main;
import dev.ritterackr.bitterlog.dao.CategoryDao;
import dev.ritterackr.bitterlog.dao.ImageDao;
import dev.ritterackr.bitterlog.dao.MemoDao;
import dev.ritterackr.bitterlog.dao.TagDao;
import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Category;
import dev.ritterackr.bitterlog.model.Memo;
import dev.ritterackr.bitterlog.model.Tag;
import dev.ritterackr.bitterlog.util.DialogHelper;
import dev.ritterackr.bitterlog.util.ExportImportManager;
import dev.ritterackr.bitterlog.util.ImageManager;
import dev.ritterackr.bitterlog.util.MarkdownRenderer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * メイン画面のコントローラークラス
 */
public class MainController implements Initializable {

    // FXMLコンポーネント - ツールバー
    @FXML private Button newMemoBtn;
    @FXML private TextField searchField;
    @FXML private Button searchBtn;
    @FXML private ToggleButton editModeBtn;
    @FXML private Button insertImageBtn;

    // FXMLコンポーネント - メニューバー
    @FXML private MenuItem newMemoMenu;
    @FXML private MenuItem exportMenu;
    @FXML private MenuItem importMenu;
    @FXML private CheckMenuItem darkModeMenu;
    @FXML private CheckMenuItem filterFavoriteMenu;
    @FXML private CheckMenuItem pinMenu;
    @FXML private CheckMenuItem favoriteMenu;
    @FXML private MenuItem addCategoryMenu;

    // FXMLコンポーネント - サイドバー
    @FXML private ComboBox<Category> categoryComboBox;
    @FXML private Button addCategoryBtn;
    @FXML private Button editCategoryBtn;
    @FXML private Button deleteCategoryBtn;
    @FXML private ListView<Tag> tagListView;
    @FXML private Button clearTagFilterBtn;

    // FXMLコンポーネント - エディタ
    @FXML private VBox editorPane;
    @FXML private SplitPane splitPane;
    @FXML private ListView<Memo> memoListView;
    @FXML private TextField titleField;
    @FXML private ComboBox<Category> memoCategoryComboBox;
    @FXML private TextField tagField;
    @FXML private CodeArea editorArea;
    @FXML private WebView previewView;

    // DAO
    private final MemoDao memoDao = new MemoDao();
    private final ImageDao imageDao = new ImageDao();
    private final CategoryDao categoryDao = new CategoryDao();
    private final TagDao tagDao = new TagDao();

    // State
    private Memo currentMemo;
    private boolean isLoading = false;
    private final JavaBridge javaBridge = new JavaBridge(this);

    private javafx.animation.PauseTransition previewDebounce =
        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));

    private javafx.animation.PauseTransition saveDebounce =
        new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        previewDebounce.setOnFinished(e -> updatePreview(editorArea.getText()));
        saveDebounce.setOnFinished(e -> saveCurrentMemoNow());
        loadMemos();
        loadCategories();
        setupMemoListView();
        setupTagListView();
        setupEditor();
        setupToolbar();
        setupMenuBar();
        setupCategoryComboBox();
        setupDarkMode();
        setupWebViewBridge();
        Platform.runLater(() -> splitPane.setDividerPositions(0.2, 0.6));
    }

    /* -----------------
        セットアップ
     ----------------- */

    /**
     * メモ一覧の設定
     */
    private void setupMemoListView() {
        memoListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    isLoading = true;
                    currentMemo = newVal;
                    titleField.setText(newVal.getTitle());
                    editorArea.replaceText(newVal.getContent() != null ? newVal.getContent() : "");
                    previewDebounce.stop();
                    javafx.animation.PauseTransition delay = new javafx.animation.PauseTransition(javafx.util.Duration.millis(100));
                    delay.setOnFinished(e -> updatePreview(newVal.getContent()));
                    delay.play();
                    pinMenu.setSelected(newVal.isPinned());
                    favoriteMenu.setSelected(newVal.isFavorite());
                    updateMemoCategoryComboBox(newVal);
                    loadTags(newVal);
                    setEditMode(false);
                    isLoading = false;
                }
            }
        );

        memoListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Memo memo, boolean empty) {
                super.updateItem(memo, empty);
                if (empty || memo == null) {
                    setText(null);
                } else {
                    setText(memo.getTitle().isEmpty() ? " (タイトルなし) " : memo.getTitle());
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("削除");
        deleteItem.setOnAction(e -> deleteCurrentMemo());
        contextMenu.getItems().add(deleteItem);
        memoListView.setContextMenu(contextMenu);
    }

    /**
     * タグ一覧の設定
     */
    private void setupTagListView() {
        loadTagList();

        tagListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Tag tag, boolean empty) {
                super.updateItem(tag, empty);
                setText(empty || tag == null ? null : "# " + tag.getName());
            }
        });

        tagListView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal == null) return;
                clearTagFilterBtn.setDisable(false);
                try {
                    List<Memo> memos = memoDao.findByTagName(newVal.getName());
                    memoListView.getItems().setAll(memos);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        );

        // 解除
        clearTagFilterBtn.setDisable(true);
        clearTagFilterBtn.setOnAction(e -> {
            tagListView.getSelectionModel().clearSelection();
            clearTagFilterBtn.setDisable(true);
            loadMemos();
        });

        // 削除
        ContextMenu tagContextMenu = new ContextMenu();
        MenuItem deleteTagItem = new MenuItem("タグを削除");
        deleteTagItem.setOnAction(e -> {
            Tag selected = tagListView.getSelectionModel().getSelectedItem();
            if (selected == null) return;

            boolean confirmed = DialogHelper.showConfirm(
                tagListView.getScene().getWindow(), "タグ削除", "「" + selected.getName() + "」を削除しますか？"
            );

            if (confirmed) {
                try {
                    tagDao.delete(selected.getId());
                    loadTagList();
                    loadMemos();
                } catch (SQLException e2) {
                    e2.printStackTrace();
                }
            }
        });
        tagContextMenu.getItems().add(deleteTagItem);
        tagListView.setContextMenu(tagContextMenu);
    }

    /**
     * エディタの設定
     */
    private void setupEditor() {
        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            if (darkModeMenu.isSelected() && editorArea.getLength() > 0) {
                editorArea.setStyleClass(0, editorArea.getLength(), "dark-text");
            }
            saveCurrentMemo();
            previewDebounce.playFromStart();
        });

        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            saveCurrentMemo();
        });

        editorArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.V) {
                pasteImageFromClipboard();
            }
        });
        editorArea.estimatedScrollYProperty().addListener((obs, oldVal, newVal) -> {
            double totalHeight = editorArea.totalHeightEstimateProperty().getValue();
            double visibleHeight = editorArea.getHeight();
            double scrollableHeight = totalHeight - visibleHeight;
            double scrollRatio = scrollableHeight > 0 ? newVal.doubleValue() / scrollableHeight : 0;
            previewView.getEngine().executeScript(
                    "var scrollableHeight = document.body.scrollHeight - window.innerHeight;" +
                            "window.scrollTo(0, scrollableHeight * " + scrollRatio + ");"
            );
        });
        editorArea.addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.TAB) {
                event.consume();
                editorArea.insertText(editorArea.getCaretPosition(), "    ");
            }
        });
        editorArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.V) {
                pasteImageFromClipboard();
            }
        });

        // メモカテゴリComboBoxの設定
        memoCategoryComboBox.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "なし" : item.getName());
            }
        });
        memoCategoryComboBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Category item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "なし" : item.getName());
            }
        });
        memoCategoryComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (currentMemo == null || isLoading) return;
                currentMemo.setCategoryId(newVal != null ? newVal.getId() : 0);
                saveCurrentMemo();
            }
        );

        // タグ変更時に保存
        tagField.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && currentMemo != null && !isLoading) saveTags();
        });
    }

    /**
     * ツールバーの設定
     */
    private void setupToolbar() {
        newMemoBtn.setOnAction(e -> createNewMemo());
        searchBtn.setOnAction(e -> searchMemos());
        insertImageBtn.setOnAction(e -> insertImageFromFile());

        // 起動時は閲覧モード
        setEditMode(false);

        editModeBtn.setOnAction(e -> setEditMode(editModeBtn.isSelected()));
    }

    /**
     * メニューバーの設定
     */
    private void setupMenuBar() {
        newMemoMenu.setOnAction(e -> createNewMemo());
        exportMenu.setOnAction(e -> exportData());
        importMenu.setOnAction(e -> importData());

        darkModeMenu.setOnAction(e -> toggleDarkMode(darkModeMenu.isSelected()));

        filterFavoriteMenu.setOnAction(e -> {
            if (filterFavoriteMenu.isSelected()) {
                filterFavorites();
            } else {
                loadMemos();
            }
        });

        pinMenu.setOnAction(e -> {
            if (currentMemo == null) return;
            currentMemo.setPinned(pinMenu.isSelected());
            saveCurrentMemo();
            loadMemos();
        });

        favoriteMenu.setOnAction(e -> {
            if (currentMemo == null) return;
            currentMemo.setFavorite(favoriteMenu.isSelected());
            saveCurrentMemo();
            loadMemos();
        });

        addCategoryMenu.setOnAction(e -> addCategory());
    }

    /**
     * カテゴリComboBoxの設定
     */
    private void setupCategoryComboBox() {
        addCategoryBtn.setOnAction(e -> addCategory());
        editCategoryBtn.setOnAction(e -> editCategory());
        deleteCategoryBtn.setOnAction(e -> deleteCategory());

        categoryComboBox.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                boolean hasSelection = newVal != null;
                editCategoryBtn.setDisable(!hasSelection);
                deleteCategoryBtn.setDisable(!hasSelection);

                if (isLoading) return;

                if (newVal == null) {
                    loadMemos();
                } else {
                    filterByCategory(newVal.getId());
                }
            }
        );
    }

    /**
     * ダークモードの初期設定
     */
    private void setupDarkMode() {
        boolean isDark = "true".equals(java.util.prefs.Preferences
            .userNodeForPackage(MainController.class)
            .get("darkMode", "false"));
        if (isDark) {
            darkModeMenu.setSelected(true);
            Platform.runLater(() -> toggleDarkMode(true));
        }
    }

    /**
     * WebViewブリッジの設定
     */
    private void setupWebViewBridge() {
        previewView.getEngine().getLoadWorker().stateProperty().addListener(
            (obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    netscape.javascript.JSObject window =
                        (netscape.javascript.JSObject) previewView.getEngine().executeScript("window");
                    window.setMember("javabridge", javaBridge);
                }
            }
        );
    }

    /* -----------------
        メモ操作メソッド
     ----------------- */

    /**
     * メモ一覧の読み込み
     */
    private void loadMemos() {
        try {
            List<Memo> memos = memoDao.findAll();
            memoListView.getItems().setAll(memos);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新規メモの作成
     */
    private void createNewMemo() {
        try {
            Memo memo = new Memo("新規メモ", "");
            int id = memoDao.create(memo);
            memo.setId(id);
            memoListView.getItems().add(0, memo);
            memoListView.getSelectionModel().select(memo);
            setEditMode(true);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * 現在のメモを保存する
     */
    private void saveCurrentMemo() {
        if (currentMemo == null || isLoading) return;
        currentMemo.setTitle(titleField.getText());
        currentMemo.setContent(editorArea.getText());
        saveDebounce.playFromStart();
    }
    private void saveCurrentMemoNow() {
        if (currentMemo == null) return;
        try {
            memoDao.update(currentMemo);
            memoListView.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }



    /**
     * 現在選択中のメモを削除する
     */
    private void deleteCurrentMemo() {
        Memo selected = memoListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // 確認ダイアログ
        boolean confirmed = DialogHelper.showConfirm(
            memoListView.getScene().getWindow(), "削除確認", "「" + selected.getTitle() + "」を削除します"
        );

        if (confirmed) {
            try {
                memoDao.delete(selected.getId());
                memoListView.getItems().remove(selected);
                currentMemo = null;
                titleField.clear();
                editorArea.clear();
                updatePreview("");
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * メモを検索
     */
    private void searchMemos() {
        String keyword = searchField.getText();
        if (keyword == null || keyword.isBlank()) {
            loadMemos();
            return;
        }
        try {
            List<Memo> results = memoDao.search(keyword);
            memoListView.getItems().setAll(results);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * お気に入りメモのみに絞り込む
     */
    private void filterFavorites() {
        try {
            List<Memo> allMemos = memoDao.findAll();
            List<Memo> favorites = allMemos.stream()
                .filter(Memo::isFavorite)
                .toList();
            memoListView.getItems().setAll(favorites);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /* -----------------
        画像操作メソッド
     ----------------- */

    /**
     * ファイルから画像を挿入
     */
    private void insertImageFromFile() {
        if (currentMemo == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("画像を選択");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("画像ファイル", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );

        File file = fileChooser.showOpenDialog(insertImageBtn.getScene().getWindow());
        if (file == null) return;

        try {
            String savePath = ImageManager.saveImage(file);
            String markdownImage = "\n![" + file.getName() + "](file:///" + savePath.replace("\\", "/") + ")\n";
            editorArea.insertText(editorArea.getCaretPosition(), markdownImage);

            dev.ritterackr.bitterlog.model.Image image =
                new dev.ritterackr.bitterlog.model.Image(currentMemo.getId(), file.getName(), savePath);
            imageDao.create(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * クリップボードから画像を貼り付け
     */
    private void pasteImageFromClipboard() {
        if (currentMemo == null) return;

        javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
        if (!clipboard.hasImage()) return;

        javafx.scene.image.Image fxImage = clipboard.getImage();
        BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(fxImage, null);

        try {
            String savedPath = ImageManager.saveImageFromClipboard(bufferedImage);
            String markdownImage = "\n![画像](file:///" + savedPath.replace("\\", "/") + ")\n";
            editorArea.insertText(editorArea.getCaretPosition(), markdownImage);

            // DBに画像を登録
            dev.ritterackr.bitterlog.model.Image image =
                new dev.ritterackr.bitterlog.model.Image(currentMemo.getId(), "clipboard_image", savedPath);
            imageDao.create(image);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* -----------------
        Export・Import
     ----------------- */

    /**
     * データをZIPファイルにエクスポート
     */
    private void exportData() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("エクスポート先を選択");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("ZIPファイル", "*.zip")
        );
        fileChooser.setInitialFileName("bitterlog_export.zip");

        File file = fileChooser.showSaveDialog(newMemoBtn.getScene().getWindow());
        if (file == null) return;

        try {
            ExportImportManager.export(file.getAbsolutePath());
            showInfo("エクスポート完了", "エクスポートが完了しました");
        } catch (Exception e) {
            e.printStackTrace();
            showError("エクスポートエラー", "エクスポートに失敗しました");
        }
    }

    /**
     * ZIPファイルからデータをインポート
     */
    private void importData() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("インポートするZIPファイルを選択");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("ZIPファイル", "*.zip")
        );

        File file = fileChooser.showOpenDialog(newMemoBtn.getScene().getWindow());
        if (file == null) return;

        try {
            ExportImportManager.importData(file.getAbsolutePath());
            loadMemos();
            showInfo("インポート完了", "インポートが完了しました");
        } catch (Exception e) {
            e.printStackTrace();
            showError("インポートエラー", "インポートに失敗しました");
        }
    }

    /* --------------------
        カテゴリ操作メソッド
     -------------------- */

    /**
     * カテゴリ一覧を読み込む<br>
     * 先頭に全ての選択肢を追加
     */
    private void loadCategories() {
        try {
            isLoading = true;
            List<Category> categories = categoryDao.findAll();
            categoryComboBox.getItems().clear();
            categoryComboBox.getItems().add(null);
            categoryComboBox.getItems().addAll(categories);
            isLoading = false;

            categoryComboBox.setCellFactory(list -> new ListCell<>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "All" : item.getName());
                }
            });

            categoryComboBox.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(Category item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty || item == null ? "All" : item.getName());
                }
            });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * カテゴリでメモを絞り込み
     */
    private void filterByCategory(int categoryId) {
        try {
            List<Memo> allMemos = memoDao.findAll();
            List<Memo> filtered = allMemos.stream()
                .filter(m -> m.getCategoryId() == categoryId)
                .toList();
            memoListView.getItems().setAll(filtered);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * カテゴリを追加
     */
    private void addCategory() {
        Optional<String> result = DialogHelper.showInput(
            addCategoryBtn.getScene().getWindow(), "カテゴリ追加", "カテゴリ名を入力してください"
        );

        result.ifPresent(name -> {
            if (name.isBlank()) return;
            try {
                categoryDao.create(new Category(name));
                loadCategories();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * カテゴリ名を編集
     */
    private void editCategory() {
        Category selected = categoryComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Optional<String> result = DialogHelper.showInput(
            addCategoryBtn.getScene().getWindow(), "カテゴリ編集", "新しいカテゴリ名を入力してください"
        );

        result.ifPresent(name -> {
            if (name.isBlank()) return;
            try {
                selected.setName(name);
                categoryDao.update(selected);
                loadCategories();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * カテゴリを削除
     */
    private void deleteCategory() {
        Category selected = categoryComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        boolean confirmed = DialogHelper.showConfirm(
            addCategoryBtn.getScene().getWindow(), "カテゴリ削除", "「" + selected.getName() + "」を削除しますか？"
        );

        if (confirmed) {
            try {
                categoryDao.delete(selected.getId());
                loadCategories();
                loadMemos();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * メモのカテゴリComboBoxを更新
     */
    private void updateMemoCategoryComboBox(Memo memo) {
        memoCategoryComboBox.getItems().clear();
        memoCategoryComboBox.getItems().add(null);
        try {
            memoCategoryComboBox.getItems().addAll(categoryDao.findAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (memo.getCategoryId() > 0) {
            memoCategoryComboBox.getItems().stream()
                .filter(c -> c != null && c.getId() == memo.getCategoryId())
                .findFirst()
                .ifPresent(memoCategoryComboBox.getSelectionModel()::select);
        } else {
            memoCategoryComboBox.getSelectionModel().select(null);
        }
    }


    /* -----------------
       タグ操作メソッド
     ----------------- */
    private void saveTags() {
        if (currentMemo == null) return;

        try {
            // 既存タグの削除
            tagDao.removeAllTagsFromMemo(currentMemo.getId());

            // 入力されたタグの保存
            String tagText = tagField.getText();
            if (tagText == null || tagText.isBlank()) return;

            String[] tagNames = tagText.split(",");
            for (String tagName : tagNames) {
                String trimmed = tagName.trim();
                if (trimmed.isBlank()) continue;
                int tagId = tagDao.create(trimmed);
                tagDao.addTagToMemo(currentMemo.getId(), tagId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        loadTagList();
    }

    /**
     * タグの読み込み
     */
    private void loadTags(Memo memo) {
        try {
            List<Tag> tags = tagDao.findByMemoId(memo.getId());
            String tagText = tags.stream()
                .map(Tag::getName)
                .collect(java.util.stream.Collectors.joining(", "));
            tagField.setText(tagText);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * タグ一覧の読み込み
     */
    private void loadTagList() {
        try {
            List<Tag> tags = tagDao.findAll();
            tagListView.getItems().setAll(tags);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /* -----------------
       表示操作メソッド
     ----------------- */

    /**
     * プレビューの更新
     */
    private void updatePreview(String markdown) {
        if (markdown == null) markdown = "";
        boolean isDark = darkModeMenu != null && darkModeMenu.isSelected();
        String html = MarkdownRenderer.render(markdown, isDark);

        Object scrollY = previewView.getEngine().executeScript(
            "typeof window !== 'undefined' ? window.scrollY : 0"
        );

        previewView.getEngine().loadContent(html, "text/html");

        previewView.getEngine().getLoadWorker().stateProperty().addListener(
            (obs, oldState, newState) -> {
                if (newState == Worker.State.SUCCEEDED) {
                    previewView.getEngine().executeScript(
                        "window.scrollTo(0, " + scrollY + ");"
                    );
                }
            }
        );
    }

    /**
     * 編集モードと閲覧モードの切り替え
     * @param editMode true: 編集モード, false: 閲覧モード
     */
    private void setEditMode(boolean editMode) {
        editorPane.setVisible(editMode);
        editorPane.setManaged(editMode);
        insertImageBtn.setVisible(editMode);
        insertImageBtn.setManaged(editMode);
        editModeBtn.setSelected(editMode);
        editModeBtn.setText(editMode ? "閲覧" : "編集");

        if (editMode) {
            // 編集モード
            if (!splitPane.getItems().contains(editorPane))
                splitPane.getItems().add(1, editorPane);
            Platform.runLater(() -> splitPane.setDividerPositions(0.2, 0.6));
        } else {
            // 閲覧モード
            splitPane.getItems().remove(editorPane);
            Platform.runLater(() -> splitPane.setDividerPositions(0.25));
        }
    }

    /**
     * ダークモードとライトモードの切り替え
     */
    private void toggleDarkMode(boolean isDark) {
        var root = previewView.getScene().getRoot();
        if (isDark) {
            root.getStyleClass().add("dark");
            editorArea.setStyleClass(0, editorArea.getLength(), "dark-text");
        } else {
            root.getStyleClass().remove("dark");
            root.getStyleClass().remove("dark-text");
        }
        darkModeMenu.setSelected(isDark);
        updatePreview(editorArea.getText());
        java.util.prefs.Preferences.userNodeForPackage(MainController.class)
                .put("darkMode", String.valueOf(isDark));
    }

    /* ---------------------
       ユーティリティメソッド
     --------------------- */

    /**
     * 情報ダイアログの表示
     */
    private void showInfo(String title, String message) {
        DialogHelper.showInfo(newMemoBtn.getScene().getWindow(), title, message);
    }

    /**
     * エラーダイアログの表示
     */
    private void showError(String title, String message) {
        DialogHelper.showError(newMemoBtn.getScene().getWindow(), title, message);
    }

    /**
     * 指定したタイトルのメモに移動
     * @param title メモタイトル
     * @param line 移動先の行番号
     */
    public void navigateToMemo(String title, String line) {
        try {
            List<Memo> memos = memoDao.findAll();
            memos.stream()
                    .filter(m -> m.getTitle().equals(title))
                    .findFirst()
                    .ifPresent(memo -> {
                        // リストビューの中から同じIDのメモを探して選択する
                        memoListView.getItems().stream()
                                .filter(item -> item.getId() == memo.getId())
                                .findFirst()
                                .ifPresent(item -> {
                                    memoListView.getSelectionModel().select(item);
                                    memoListView.scrollTo(item);
                                });

                        if (line != null && !line.isEmpty()) {
                            // 指定行にスクロールする
                            int lineNum = Integer.parseInt(line) - 1;
                            String content = editorArea.getText();
                            String[] lines = content.split("\n");
                            if (lineNum >= 0 && lineNum < lines.length) {
                                int pos = 0;
                                for (int i = 0; i < lineNum; i++) {
                                    pos += lines[i].length() + 1;
                                }
                                editorArea.moveTo(pos);
                                editorArea.requestFollowCaret();
                            }
                        }
                    });
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * WebView と Java を繋ぐブリッジクラス
     */
    public static class JavaBridge {

        private MainController controller;

        public JavaBridge(MainController controller) {
            this.controller = controller;
        }

        /**
         * テキストをクリップボードにコピーする
         * @param text コピーするテキスト
         */
        public void copyToClipboard(String text) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(text);
            clipboard.setContent(content);
        }

        /**
         * メモリンクがクリックされたときに呼ばれる
         */
        public void openMemoLink(String title, String line) {
            javafx.application.Platform.runLater(() -> controller.navigateToMemo(title, line));
        }
    }
}
