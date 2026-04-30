package dev.ritterackr.bitterlog;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;

public class BitterLogApplication extends Application {

    @Override
    public void init() {
        // アプリ起動時にDB初期化
        DatabaseManager.getInstance();
    }

    @Override
    public void start(Stage stage) throws IOException {
        // 仮描画
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 800, 600);
        stage.setTitle("BitterLog");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // アプリ終了時にDB接続をクローズ
        DatabaseManager.getInstance().closeConnection();
    }

    public static void main(String[] args) {
        launch();
    }
}
