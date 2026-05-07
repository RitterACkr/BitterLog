package dev.ritterackr.bitterlog.controller;

import dev.ritterackr.bitterlog.dao.ImageDao;
import dev.ritterackr.bitterlog.dao.MemoDao;
import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Memo;
import dev.ritterackr.bitterlog.util.ExportImportManager;
import dev.ritterackr.bitterlog.util.ImageManager;
import dev.ritterackr.bitterlog.util.MarkdownRenderer;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import org.fxmisc.richtext.CodeArea;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

/**
 * メイン画面のコントローラークラス
 */
public class MainController implements Initializable {

    @FXML private Button newMemoBtn;
    @FXML private TextField searchField;
    @FXML private Button insertImageBtn;
    @FXML private Button searchBtn;
    @FXML private Button exportBtn;
    @FXML private Button importBtn;
    @FXML private ToggleButton pinBtn;
    @FXML private ToggleButton favoriteBtn;
    @FXML private SplitPane splitPane;
    @FXML private ListView<Memo> memoListView;
    @FXML private TextField titleField;
    @FXML private CodeArea editorArea;
    @FXML private WebView previewView;

    private final MemoDao memoDao = new MemoDao();
    private final ImageDao imageDao = new ImageDao();

    private Memo currentMemo;
    private boolean isLoading = false;
    private final JavaBridge javaBridge = new JavaBridge();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // メモ一覧の読み込み
        loadMemos();

        // メモ一覧で選択した際にエディタにも表示
        memoListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        isLoading = true;
                        currentMemo = newVal;
                        titleField.setText(newVal.getTitle());
                        editorArea.replaceText(newVal.getContent() != null ? newVal.getContent() : "");
                        updatePreview(newVal.getContent());
                        pinBtn.setSelected(newVal.isPinned());
                        favoriteBtn.setSelected(newVal.isFavorite());
                        isLoading = false;
                    }
                }
        );;

        // エディタの内容変更時にプレビューを更新
        editorArea.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePreview(newVal);
            saveCurrentMemo();
        });

        // タイトル変更時に保存
        titleField.textProperty().addListener((obs, oldVal, newVal) -> {
            saveCurrentMemo();
        });

        // 新規メモ作成ボタン
        newMemoBtn.setOnAction(e -> createNewMemo());

        // 画像挿入ボタン
        insertImageBtn.setOnAction(e -> insertImageFromFile());

        editorArea.setOnKeyPressed(event -> {
            if (event.isControlDown() && event.getCode() == javafx.scene.input.KeyCode.V) {
                pasteImageFromClipboard();
            }
        });

        // 検索ボタン
        searchBtn.setOnAction(e -> searchMemos());

        // Exportボタン
        exportBtn.setOnAction(e -> exportData());

        // Importボタン
        importBtn.setOnAction(e -> importData());

        // ピン留めボタン
        pinBtn.setOnAction(e -> {
            if (currentMemo == null) return;
            currentMemo.setPinned(pinBtn.isSelected());
            saveCurrentMemo();
            loadMemos();
        });

        // お気に入りボタン
        favoriteBtn.setOnAction(e -> {
            if (currentMemo == null) return;
            currentMemo.setFavorite(favoriteBtn.isSelected());
            saveCurrentMemo();
            loadMemos();
        });

        // メモ一覧のセルの表示を調整
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
        // メモ一覧の右クリックメニュー
        ContextMenu contextMenu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("削除");
        deleteItem.setOnAction(e -> deleteCurrentMemo());
        contextMenu.getItems().add(deleteItem);
        memoListView.setContextMenu(contextMenu);

        // SplitPaneの分割位置を指定
        Platform.runLater(() -> splitPane.setDividerPositions(0.2, 0.6));

        // JavaブリッジをWebViewに登録する
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
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("削除確認");
        alert.setHeaderText("メモを削除しますか?");
        alert.setContentText("「" + selected.getTitle() + "」を削除します");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
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
        });
    }

    /**
     * 現在のメモを保存する
     */
    private void saveCurrentMemo() {
        if (currentMemo == null || isLoading) return;
        try {
            currentMemo.setTitle(titleField.getText());
            currentMemo.setContent(editorArea.getText());
            memoDao.update(currentMemo);
            memoListView.refresh();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * プレビューの更新
     */
    private void updatePreview(String markdown) {
        if (markdown == null) markdown = "";
        String html = MarkdownRenderer.render(markdown);
        previewView.getEngine().loadContent(html, "text/html");
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

            // DBに画像を登録
            try (var stmt = DatabaseManager.getInstance().getConnection().createStatement();
                 var rs = stmt.executeQuery("PRAGMA foreign_keys")) {
                if (rs.next()) System.out.println("本番foreign_keys: " + rs.getInt(1));
            }
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

        File file = fileChooser.showSaveDialog(exportBtn.getScene().getWindow());
        if (file == null) return;

        try {
            ExportImportManager.export(file.getAbsolutePath());
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("エクスポート完了");
            alert.setHeaderText(null);
            alert.setContentText("エクスポートが完了しました");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("エクスポートエラー");
            alert.setHeaderText(null);
            alert.setContentText("エクスポートに失敗しました");
            alert.showAndWait();
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

        File file = fileChooser.showOpenDialog(importBtn.getScene().getWindow());
        if (file == null) return;

        try {
            ExportImportManager.importData(file.getAbsolutePath());
            loadMemos();
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("インポート完了");
            alert.setHeaderText(null);
            alert.setContentText("インポートが完了しました");
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("インポートエラー");
            alert.setHeaderText(null);
            alert.setContentText("インポートに失敗しました");
            alert.showAndWait();
        }
    }


    /**
     * WebView と Java を繋ぐブリッジクラス
     */
    public static class JavaBridge {
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
    }
}
