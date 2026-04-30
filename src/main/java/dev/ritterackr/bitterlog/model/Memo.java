package dev.ritterackr.bitterlog.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Memoモデルクラス
 */
public class Memo {

    /** メモID */
    private int id;

    /** タイトル */
    private String title;

    /** 内容 */
    private String content;

    /** カテゴリID */
    private int categoryId;

    /** ピン留めフラグ */
    private boolean isPinned;

    /** お気に入りフラグ */
    private boolean isFavorite;

    /** 作成日時 */
    private LocalDateTime createdAt;

    /** 更新日時 */
    private LocalDateTime updatedAt;

    /** タグリスト */
    private List<String> tags;


    /**
     * 新規メモ作成用コンストラクタ
     */
    public Memo(String title, String content) {
        this.title = title;
        this.content = content;
        this.isPinned = false;
        this.isFavorite = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * DB取得用コンストラクタ
     */
    public Memo(int id, String title, String content, int categoryId,
                boolean isPinned, boolean isFavorite,
                LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.categoryId = categoryId;
        this.isPinned = isPinned;
        this.isFavorite = isFavorite;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }

    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean pinned) { isPinned = pinned; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { this.isFavorite = favorite; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
