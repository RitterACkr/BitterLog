package dev.ritterackr.bitterlog;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
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
        // WebViewでローカルファイルへのアクセスを許可する
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
        FXMLLoader fxmlLoader = new FXMLLoader(
                BitterLogApplication.class.getResource("main-view.fxml")
        );
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(
            BitterLogApplication.class.getResource("style.css").toExternalForm()
        );
        stage.setTitle("BitterLog");
        stage.setScene(scene);
        stage.setMaximized(true);
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
