package dev.ritterackr.bitterlog.model;

/**
 * Tagモデルクラス
 */
public class Tag {

    /** タグID */
    private int id;

    /** タグ名 */
    private String name;

    /**
     * 新規タグ作成用コンストラクタ
     */
    public Tag(String name) {
        this.name = name;
    }

    /**
     * DB取得用コンストラクタ
     */
    public Tag(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
