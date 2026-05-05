package dev.ritterackr.bitterlog.model;

import java.time.LocalDateTime;

/**
 * 画像モデルクラス
 */
public class Image {

    /** 画像ID */
    private int id;

    /** 関連するメモID */
    private int memoId;

    /** ファイル名 */
    private String fileName;

    /** ファイルパス */
    private String filePath;

    /** 作成日時 */
    private LocalDateTime createdAt;


    /**
     * 新規画像作成用コンストラクタ
     */
    public Image(int memoId, String fileName, String filePath) {
        this.memoId = memoId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * DB取得用コンストラクタ
     */
    public Image(int id, int memoId, String fileName, String filePath, LocalDateTime createdAt) {
        this.id = id;
        this.memoId = memoId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getMemoId() { return memoId; }
    public void setMemoId(int memoId) { this.memoId = memoId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
