package dev.ritterackr.bitterlog.model;

import java.time.LocalDateTime;

/**
 * カテゴリモデルクラス
 */
public class Category {

    /** カテゴリID */
    private int id;

    /** カテゴリ名 */
    private String name;

    /** 作成日時 */
    private LocalDateTime createdAt;

    /**
     * 新規カテゴリ作成用
     */
    public Category(String name) {
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * DB取得用
     */
    public Category(int id, String name, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name; }
}
