package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Image;
import org.flywaydb.core.internal.database.base.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 画像のCRUD操作を担当するDAOクラス
 */
public class ImageDao {

    /**
     * 画像の登録
     * @param image 登録する画像
     * @return 生成された画像ID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public int create(Image image) throws SQLException {
        String sql = """
            INSERT INTO images (memo_id, file_name, file_path, created_at)
            VALUES (?, ?, ?, ?)
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, image.getMemoId());
            stmt.setString(2, image.getFileName());
            stmt.setString(3, image.getFilePath());
            stmt.setString(4, LocalDateTime.now().toString());
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * メモIDに紐づく画像リストの取得
     * @param memoId メモID
     * @return 画像リスト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Image> findByMemoId(int memoId) throws SQLException {
        String sql = "SELECT * FROM images WHERE memo_id = ? ORDER BY created_at ASC";

        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Image> images = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) images.add(mapResultSet(rs));
            }
        }
        return images;
    }

    /**
     * 画像を削除
     * @param id 削除する画像ID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM images WHERE id = ?";

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * メモIDに紐づく全画像を削除<br>
     * メモ削除時に使用
     * @param memoId メモID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void deleteByMemoId(int memoId) throws SQLException {
        String sql = "DELETE FROM images WHERE memo_id = ?";

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            stmt.executeUpdate();
        }
    }

    /**
     * ResultSet から Image オブジェクトにマッピングする
     * @param rs ResultSet
     * @return Imageオブジェクト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    private Image mapResultSet(ResultSet rs) throws SQLException {
        return new Image(
            rs.getInt("id"),
            rs.getInt("memo_id"),
            rs.getString("file_name"),
            rs.getString("file_path"),
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
