package dev.ritterackr.bitterlog.controller;

import dev.ritterackr.bitterlog.dao.MemoDao;
import dev.ritterackr.bitterlog.model.Memo;
import dev.ritterackr.bitterlog.util.MarkdownRenderer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import org.fxmisc.richtext.CodeArea;

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
    @FXML private Button searchBtn;
    @FXML private SplitPane splitPane;
    @FXML private ListView<Memo> memoListView;
    @FXML private TextField titleField;
    @FXML private CodeArea editorArea;
    @FXML private WebView previewView;

    private final MemoDao memoDao = new MemoDao();
    private Memo currentMemo;
    private boolean isLoading = false;

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

        // 検索ボタン
        searchBtn.setOnAction(e -> searchMemos());

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

        // SplitPaneの分割位置を指定
        Platform.runLater(() -> splitPane.setDividerPositions(0.2, 0.6));
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
        previewView.getEngine().loadContent(html);
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
}
