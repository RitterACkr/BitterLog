package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Tag;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * タグのCRUD操作を担当するクラス
 */
public class TagDao {

    /**
     * タグの新規作成<br>
     * 既存の場合はそのIDを返す
     * @param name タグ名
     * @return 生成したID | 既存のタグID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public int create(String name) throws SQLException {
        Tag existing = findByName(name);
        if (existing != null) return existing.getId();

        String sql = "INSERT INTO tags (name) VALUES (?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, name);
            stmt.executeUpdate();

            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }

        return -1;
    }

    /**
     * タグを削除
     * @param id 削除するタグID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM tags WHERE id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * 全タグの取得
     * @return タグ一覧
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Tag> findAll() throws SQLException {
        String sql = "SELECT * FROM tags ORDER BY name ASC";
        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) tags.add(new Tag(rs.getInt("id"), rs.getString("name")));
        }
        return tags;
    }

    /**
     * タグ名を指定してタグを取得
     * @param name タグ名
     * @return タグ (存在しない場合はnull)
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public Tag findByName(String name) throws SQLException {
        String sql = "SELECT * FROM tags WHERE name = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Tag(rs.getInt("id"), rs.getString("name"));
                }
            }
        }
        return null;
    }

    /**
     * 指定したメモのタグリストを取得
     * @param memoId メモID
     * @return タグリスト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Tag> findByMemoId(int memoId) throws SQLException {
        String sql = """
            SELECT t.* FROM tags t 
            INNER JOIN memo_tags mt ON t.id = mt.tag_id 
            WHERE mt.memo_id = ?
        """;
        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Tag> tags = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            try (ResultSet rs = stmt.executeQuery()) {
                while(rs.next()) tags.add(new Tag(rs.getInt("id"), rs.getString("name")));
            }
        }
        return tags;
    }

    /**
     * メモにタグを設定
     * @param memoId メモID
     * @param tagId タグID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void addTagToMemo(int memoId, int tagId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO memo_tags (memo_id, tag_id) VALUES (?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            stmt.setInt(2, tagId);
            stmt.executeUpdate();
        }
    }

    /**
     * メモからタグを削除
     * @param memoId メモID
     * @param tagId タグID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void removeTagFromMemo(int memoId, int tagId) throws SQLException {
        String sql = "DELETE FROM memo_tags WHERE memo_id = ? AND tag_id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            stmt.setInt(2, tagId);
            stmt.executeUpdate();
        }
    }

    /**
     * メモの全タグを削除する<br>
     * メモ削除 | 全タグの更新時 など
     * @param memoId メモID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void removeAllTagsFromMemo(int memoId) throws SQLException {
        String sql = "DELETE FROM memo_tags WHERE memo_id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, memoId);
            stmt.executeUpdate();
        }
    }
}
