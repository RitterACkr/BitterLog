package dev.ritterackr.bitterlog.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.util.Optional;

/**
 * BitterLogテーマに統一したカスタムダイアログを提供
 */
public class DialogHelper {

    /**
     * 確認ダイアログを表示
     * @param owner 親ウィンドウ
     * @param title タイトル
     * @param message メッセージ
     * @return OKが押された場合trueを返す
     */
    public static boolean showConfirm(Window owner, String title, String message) {
        Stage dialog = createDialog(owner, title);
        final boolean[] result = {false};

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("dialog-content");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("dialog-message");

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("キャンセル");
        cancelBtn.getStyleClass().add("dialog-btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-btn-primary");
        okBtn.setOnAction(e -> {
            result[0] = true;
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, okBtn);
        content.getChildren().addAll(messageLabel, buttons);

        setupScene(owner, dialog, content);
        dialog.showAndWait();
        return result[0];
    }

    /**
     * テキスト入力ダイアログの表示
     * @param owner 親ウィンドウ
     * @param title タイトル
     * @param message メッセージ
     * @return 入力されたテキスト
     */
    public static Optional<String> showInput(Window owner, String title, String message) {
        Stage dialog = createDialog(owner, title);
        final String[] result = {null};

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("dialog-content");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("dialog-message");

        TextField inputField = new TextField();
        inputField.getStyleClass().add("dialog-input");
        inputField.setPrefWidth(300);

        HBox buttons = new HBox(12);
        buttons.setAlignment(Pos.CENTER);

        Button cancelBtn = new Button("キャンセル");
        cancelBtn.getStyleClass().add("dialog-btn-secondary");
        cancelBtn.setOnAction(e -> dialog.close());

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-btn-primary");
        okBtn.setOnAction(e -> {
            result[0] = inputField.getText();
            dialog.close();
        });

        inputField.setOnAction(e -> {
            result[0] = inputField.getText();
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, okBtn);
        content.getChildren().addAll(messageLabel, inputField, buttons);

        setupScene(owner, dialog, content);
        dialog.showAndWait();

        return Optional.ofNullable(result[0]);
    }

    /**
     * 情報ダイアログの表示
     * @param owner 親ウィンドウ
     * @param title タイトル
     * @param message メッセージ
     */
    public static void showInfo(Window owner, String title, String message) {
        Stage dialog = createDialog(owner, title);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("dialog-content");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("dialog-message");

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-btn-primary");
        okBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(messageLabel, okBtn);

        setupScene(owner, dialog, content);
        dialog.showAndWait();
    }

    /**
     * エラーダイアログの表示
     * @param owner 親ウィンドウ
     * @param title タイトル
     * @param message メッセージ
     */
    public static void showError(Window owner, String title, String message) {
        Stage dialog = createDialog(owner, title);

        VBox content = new VBox(16);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(24));
        content.getStyleClass().add("dialog-content");

        Label messageLabel = new Label(message);
        messageLabel.setWrapText(true);
        messageLabel.getStyleClass().add("dialog-message-error");

        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("dialog-btn-primary");
        okBtn.setOnAction(e -> dialog.close());

        content.getChildren().addAll(messageLabel, okBtn);

        setupScene(owner, dialog, content);
        dialog.showAndWait();
    }


    /**
     * ダイアログ用のStageを作成
     */
    private static Stage createDialog(Window owner, String title) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.WINDOW_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.UNDECORATED);
        dialog.setTitle(title);
        return dialog;
    }

    /**
     * ダイアログ用のSceneを設定
     */
    private static void setupScene(Window owner, Stage dialog, VBox content) {
        // タイトルバー
        Label titleLabel = new Label(dialog.getTitle());
        titleLabel.getStyleClass().add("dialog-title");
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(12, 16, 12, 16));

        VBox root = new VBox();
        root.getStyleClass().add("dialog-root");
        root.getChildren().addAll(titleLabel, content);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
            DialogHelper.class.getResource("/dev/ritterackr/bitterlog/style.css").toExternalForm()
        );

        // 親ウィンドウのモードを考慮
        if (owner instanceof Stage ownerStage) {
            if (ownerStage.getScene().getRoot().getStyleClass().contains("dark")) {
                root.getStyleClass().add("dark");
            }
        }

        dialog.setScene(scene);
        dialog.setMinWidth(350);
    }
}
