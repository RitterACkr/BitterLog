package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Memo;
import org.flywaydb.core.internal.database.base.Database;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * メモのCRUD操作を担当するクラス
 */
public class MemoDao {

    /**
     * メモの新規作成
     * @param memo 作成するメモ
     * @return 生成されたMemo
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public int create(Memo memo) throws SQLException {
        String sql = """
            INSERT INTO memos(title, content, category_id, is_pinned, is_favorite, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, memo.getTitle());
            stmt.setString(2, memo.getContent());
            stmt.setInt(3, memo.getCategoryId());
            stmt.setInt(4, memo.isPinned() ? 1 : 0);
            stmt.setInt(5, memo.isFavorite() ? 1 : 0);
            stmt.setString(6, memo.getCreatedAt().toString());
            stmt.setString(7, memo.getUpdatedAt().toString());
            stmt.executeUpdate();

            // 生成されたIDを返す
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * 全メモを取得<br>
     * ピン留めメモを先頭にした後,更新日時の降順で返す
     * @return メモ一覧
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Memo> findAll() throws SQLException {
        String sql = """
            SELECT * FROM memos ORDER BY is_pinned DESC, updated_at DESC
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Memo> memos = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) memos.add(mapResultSet(rs));
        }
        return memos;
    }

    /**
     * 指定されたIDからメモを1件取得
     * @param id メモID
     * @return 該当メモ
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public Memo findById(int id) throws SQLException {
        String sql = """
            SELECT * FROM memos WHERE id = ?
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapResultSet(rs);
            }
        }
        return null;
    }

    /**
     * メモを更新
     * @param memo 更新するメモ
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void update(Memo memo) throws SQLException {
        String sql = """
            UPDATE memos SET title = ?, content = ?, category_id = ?, is_pinned = ?, is_favorite = ?, updated_at = ? WHERE id = ?
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, memo.getTitle());
            stmt.setString(2, memo.getContent());
            stmt.setInt(3, memo.getCategoryId());
            stmt.setInt(4, memo.isPinned() ? 1 : 0);
            stmt.setInt(5, memo.isFavorite() ? 1 : 0);
            stmt.setString(6, LocalDateTime.now().toString());
            stmt.setInt(7, memo.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * メモを削除<br>
     * 関連するタグ・画像ファイル・画像DBレコードも合わせて削除
     * @param id 削除するメモID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void delete(int id) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        // 画像ファイル
        ImageDao imageDao = new ImageDao();
        try {
            List<dev.ritterackr.bitterlog.model.Image> images = imageDao.findByMemoId(id);
            for (dev.ritterackr.bitterlog.model.Image image : images) {
                java.nio.file.Files.deleteIfExists(
                    java.nio.file.Paths.get(image.getFilePath())
                );
            }
            // 画像DBレコードを削除
            imageDao.deleteByMemoId(id);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // memo_tagsを削除
        String deleteTagsSql = "DELETE FROM memo_tags WHERE memo_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteTagsSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }

        // メモを削除
        String sql = "DELETE FROM memos WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * ResultSetからMemoオブジェクトにマッピング
     * @param rs ResultSet
     * @return Memoオブジェクト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    private Memo mapResultSet(ResultSet rs) throws SQLException {
        return new Memo(
                rs.getInt("id"),
                rs.getString("title"),
                rs.getString("content"),
                rs.getInt("category_id"),
                rs.getInt("is_pinned") == 1,
                rs.getInt("is_favorite") == 1,
                LocalDateTime.parse(rs.getString("created_at")),
                LocalDateTime.parse(rs.getString("updated_at"))
        );
    }

    /**
     * タイトル | 本文 でメモを検索
     * @param keyword 検索キーワード
     * @return 検索結果のメモリスト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Memo> search(String keyword) throws SQLException {
        String sql = """
            SELECT * FROM memos WHERE title LIKE ? OR content LIKE ?
            ORDER BY is_pinned DESC, updated_at DESC
        """;
        String pattern = "%" + keyword + "%";

        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Memo> memos = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) memos.add(mapResultSet(rs));
            }
        }
        return memos;
    }

    /**
     * タグ名からメモを検索
     * @param tagName タグ名
     * @return 検索結果のメモリスト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Memo> findByTagName(String tagName) throws SQLException {
        String sql = """
            SELECT m.* FROM memos m
            INNER JOIN memo_tags mt ON m.id = mt.memo_id
            INNER JOIN tags t ON mt.tag_id = t.id
            WHERE t.name = ?
            ORDER BY m.is_pinned DESC, m.updated_at DESC
        """;

        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Memo> memos = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tagName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) memos.add(mapResultSet(rs));
            }
        }
        return memos;
    }
}
