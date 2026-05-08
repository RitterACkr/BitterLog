package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Category;

import javax.xml.transform.Result;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * カテゴリのCRUD操作を担当
 */
public class CategoryDao {

    /**
     * カテゴリの新規作成
     * @param category 作成するカテゴリ
     * @return 生成されたカテゴリID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public int create(Category category) throws SQLException {
        String sql = "INSERT INTO categories (name, created_at) VALUES (?, ?)";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, category.getName());
            stmt.setString(2, LocalDateTime.now().toString());
            stmt.executeUpdate();
            try (ResultSet keys = stmt.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    /**
     * 全カテゴリの取得
     * @return カテゴリリスト
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public List<Category> findAll() throws SQLException {
        String sql = "SELECT * FROM categories ORDER BY name ASC";
        Connection conn = DatabaseManager.getInstance().getConnection();
        List<Category> categories = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) categories.add(mapResultSet(rs));
        }
        return categories;
    }

    /**
     * IDでカテゴリを取得
     * @param id カテゴリID
     * @return カテゴリ
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public Category findById(int id) throws SQLException {
        String sql = "SELECT * FROM categories WHERE id = ?";
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
     * カテゴリ名の更新
     * @param category 更新するカテゴリ
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void update(Category category) throws SQLException {
        String sql = "UPDATE categories SET name = ? WHERE id = ?";
        Connection conn = DatabaseManager.getInstance().getConnection();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category.getName());
            stmt.setInt(2, category.getId());
            stmt.executeUpdate();
        }
    }

    /**
     * カテゴリを削除 <br>
     * 関連するメモの category_id を NULL に更新
     * @param id 削除するカテゴリID
     * @throws SQLException DB操作失敗時のスタックトレース
     */
    public void delete(int id) throws SQLException {
        Connection conn = DatabaseManager.getInstance().getConnection();

        String updateMemosSql = "UPDATE memos SET category_id = 0 WHERE category_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(updateMemosSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }

        String deleteSql = "DELETE FROM categories WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteSql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    /**
     * ResultSet から Categoryオブジェクト にマッピング
     */
    private Category mapResultSet(ResultSet rs) throws SQLException {
        return new Category(
            rs.getInt("id"),
            rs.getString("name"),
            LocalDateTime.parse(rs.getString("created_at"))
        );
    }
}
