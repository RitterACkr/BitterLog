package dev.ritterackr.bitterlog.database;

import org.flywaydb.core.Flyway;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * データベース接続・初期化を管理するシングルトンクラス<br>
 * アプリ起動時にFlywayマイグレーションを実行 -> DB接続を提供
 */
public class DatabaseManager {

    /** DBファイルを保存するディレクトリ */
    private static final String DB_DIR = System.getProperty("user.home") + "/BitterLog";

    /** DBファイルのパス */
    private static final String DB_PATH = DB_DIR + "/bitterlog.db";

    /** JDBC接続URL */
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;

    /** シングルトンインスタンス */
    private static DatabaseManager instance;

    /** DB接続 */
    private Connection connection;

    /**
     * DBディレクトリの作成とマイグレーションの実行
     */
    private DatabaseManager() {
        initDirectory();
        runMigrations();
    }

    /**
     * シングルトンインスタンスを返す<br>
     * 初回呼び出し時にインスタンスを生成
     * @return DatabaseManagerインスタンス
     */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * DBファイルを保存するディレクトリを作成<br>
     * 既存の場合はなにも実行しない
     */
    private void initDirectory() {
        Path dir = Paths.get(DB_DIR);
        dir.toFile().mkdirs();
    }

    /**
     * Flywayを使ってDBマイグレーションを実行<br>
     * 未適用のマイグレーションスクリプトを自動で適用
     */
    private void runMigrations() {
        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, null, null)
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    /**
     * DB接続を返す<br>
     * 接続が未確立|クローズされている場合は新たに接続
     * @return DB接続
     * @throws SQLException 接続失敗時のスタックトレース
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(DB_URL);
        }
        return connection;
    }

    /**
     * DB接続をクローズ<br>
     * アプリ終了時に呼び出す
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
