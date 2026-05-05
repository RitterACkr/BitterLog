package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Memo;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MemoDaoのテストクラス
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MemoDaoTest {

    private static MemoDao memoDao;

    @BeforeAll
    static void setUp() {
        DatabaseManager.getInstance().closeConnection();
        try {
            DatabaseManager.getInstance().getConnection();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        memoDao = new MemoDao();
    }

    @AfterAll
    static void tearDown() {
        DatabaseManager.getInstance().closeConnection();
    }

    @Test
    @Order(1)
    @DisplayName("メモを新しく作成できるか")
    void testCreate() throws SQLException {
        Memo memo = new Memo("Title!", "Content!");
        int id = memoDao.create(memo);
        assertTrue(id > 0, "生成されたIDが1以上か");
    }

    @Test
    @Order(2)
    @DisplayName("全メモを取得できるか")
    void testFindAll() throws SQLException {
        List<Memo> memos = memoDao.findAll();
        assertFalse(memos.isEmpty(), "メモリストが空でないか");
    }

    @Test
    @Order(3)
    @DisplayName("ID指定でメモを取得できるか")
    void testFindById() throws SQLException {
        Memo memo = new Memo("検索テスト", "検索内容");
        int id = memoDao.create(memo);

        Memo found = memoDao.findById(id);
        assertNotNull(found, "メモが取得できるか");
        assertEquals("検索テスト", found.getTitle(), "タイトルが一致するか");
    }

    @Test
    @Order(4)
    @DisplayName("メモを更新できるか")
    void testUpdate() throws SQLException {
        Memo memo = new Memo("更新前タイトル", "更新前内容");
        int id = memoDao.create(memo);

        Memo created = memoDao.findById(id);
        created.setTitle("更新後タイトル");
        created.setContent("更新後内容");
        memoDao.update(created);

        Memo updated = memoDao.findById(id);
        assertEquals("更新後タイトル", updated.getTitle(), "タイトルが更新されているか");
        assertEquals("更新後内容", updated.getContent(), "内容が更新されているか");
    }

    @Test
    @Order(5)
    @DisplayName("メモを削除できるか・関連するタグや画像も削除されるか")
    void testDelete() throws SQLException {
        Memo memo = new Memo("削除テスト", "削除内容");
        int memoId = memoDao.create(memo);
        assertTrue(memoId > 0, "memoIdが正常か");
        assertNotNull(memoDao.findById(memoId), "作成直後にDBから取得できるか");

        TagDao tagDao = new TagDao();
        int tagId = tagDao.create("DeleteTestTag");
        tagDao.addTagToMemo(memoId, tagId);

        ImageDao imageDao = new ImageDao();
        // 外部キー制約を一時的に無効にして画像DBレコードを登録する
        try (var stmt = DatabaseManager.getInstance().getConnection().createStatement()) {
            stmt.execute("PRAGMA foreign_keys = OFF");
            dev.ritterackr.bitterlog.model.Image image =
                    new dev.ritterackr.bitterlog.model.Image(memoId, "test.png", "/dummy/path/test.png");
            imageDao.create(image);
            stmt.execute("PRAGMA foreign_keys = ON");
        }

        memoDao.delete(memoId);

        assertNull(memoDao.findById(memoId), "削除後にnullが返るか");

        List<dev.ritterackr.bitterlog.model.Tag> tags = tagDao.findByMemoId(memoId);
        assertTrue(tags.isEmpty(), "メモ削除後にタグの紐づきが削除されているか");

        List<dev.ritterackr.bitterlog.model.Image> images = imageDao.findByMemoId(memoId);
        assertTrue(images.isEmpty(), "メモ削除後に画像DBレコードが削除されているか");
    }

    @Test
    @Order(6)
    @DisplayName("キーワードからメモを検索できるか")
    void testSearch() throws SQLException {
        Memo memo = new Memo("検索キーワードテスト", "特定の内容ABC");
        memoDao.create(memo);

        List<Memo> results = memoDao.search("ABC");
        assertFalse(results.isEmpty(), "検索結果が空でないか");
        assertTrue(results.stream().anyMatch(m -> m.getContent().contains("ABC")),
                   "検索キーワードを含む面が結果に含まれるか");
    }

    @Test
    @Order(7)
    @DisplayName("タグ名でメモを検索できるか")
    void testFindByTagName() throws SQLException {
        Memo memo = new Memo("タグ検索テスト", "内容");
        int memoId = memoDao.create(memo);

        TagDao tagDao = new TagDao();
        int tagId = tagDao.create("SearchTag");
        tagDao.addTagToMemo(memoId, tagId);

        List<Memo> results = memoDao.findByTagName("SearchTag");
        assertFalse(results.isEmpty(), "検索結果が空でないか");
        assertTrue(results.stream().anyMatch(m -> m.getId() == memoId),
                   "タグが設定されたメモが結果に含まれるか");
    }
}
