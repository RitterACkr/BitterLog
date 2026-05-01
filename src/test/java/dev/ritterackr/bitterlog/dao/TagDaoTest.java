package dev.ritterackr.bitterlog.dao;

import dev.ritterackr.bitterlog.database.DatabaseManager;
import dev.ritterackr.bitterlog.model.Memo;
import dev.ritterackr.bitterlog.model.Tag;
import org.junit.jupiter.api.*;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TagDaoのテストクラス
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TagDaoTest {

    private static TagDao tagDao;
    private static MemoDao memoDao;

    @BeforeAll
    static void setUp() {
        DatabaseManager.getInstance();
        tagDao = new TagDao();
        memoDao = new MemoDao();
    }

    @AfterAll
    static void tearDown() {
        DatabaseManager.getInstance().closeConnection();
    }

    @Test
    @Order(1)
    @DisplayName("タグが新しく作成できるか")
    void testCreate() throws SQLException {
        int id = tagDao.create("Java");
        assertTrue(id > 0, "生成されたIDが1以上か");
    }

    @Test
    @Order(2)
    @DisplayName("同名タグは重複作成されずに既存IDが返るか")
    void testCreateDuplicate() throws SQLException {
        int id1 = tagDao.create("Python");
        int id2 = tagDao.create("Python");
        assertEquals(id1, id2, "同名タグは同じIDが返るか");
    }

    @Test
    @Order(3)
    @DisplayName("全タグを取得できるか")
    void testFindAll() throws SQLException {
        List<Tag> tags = tagDao.findAll();
        assertFalse(tags.isEmpty(), "タグリストが空でないか");
    }

    @Test
    @Order(4)
    @DisplayName("タグ名を指定してタグが取得できるか")
    void testFindByName() throws SQLException {
        tagDao.create("Kotlin");
        Tag tag = tagDao.findByName("Kotlin");
        assertNotNull(tag, "タグが取得できるか");
        assertEquals("Kotlin", tag.getName(), "タグ名が一致するか");
    }

    @Test
    @Order(5)
    @DisplayName("メモにタグが設定できるか")
    void testAddTagToMemo() throws SQLException {
        Memo memo = new Memo("タグテスト用メモ", "内容");
        int memoId = memoDao.create(memo);
        int tagId = tagDao.create("TestTag");

        tagDao.addTagToMemo(memoId, tagId);

        List<Tag> tags = tagDao.findByMemoId(memoId);
        assertFalse(tags.isEmpty(), "メモにタグが設定されているか");
        assertEquals("TestTag", tags.get(0).getName(), "タグ名が一致するか");
    }

    @Test
    @Order(6)
    @DisplayName("メモからタグを削除できるか")
    void testRemoveTagFromMemo() throws SQLException {
        Memo memo = new Memo("タグ削除テスト用メモ", "内容");
        int memoId = memoDao.create(memo);
        int tagId = tagDao.create("RemoveTag");

        tagDao.addTagToMemo(memoId, tagId);
        tagDao.removeTagFromMemo(memoId, tagId);

        List<Tag> tags = tagDao.findByMemoId(memoId);
        assertTrue(tags.isEmpty(), "タグが削除されているか");
    }

    @Test
    @Order(7)
    @DisplayName("タグを削除できるか")
    void testDelete() throws SQLException {
        int id = tagDao.create("DeleteTag");
        tagDao.delete(id);
        Tag tag = tagDao.findByName("DeleteTag");
        assertNull(tag, "削除後はnullが返るか");
    }

    @Test
    @Order(8)
    @DisplayName("タグIDでメモIDリストを取得できるか")
    void testFindMemoIdsByTagId() throws SQLException {
        Memo memo1 = new Memo("絞り込みテスト1", "内容1");
        Memo memo2 = new Memo("絞り込みテスト2", "内容2");
        int memoId1 = memoDao.create(memo1);
        int memoId2 = memoDao.create(memo2);
        int tagId = tagDao.create("FilterTag");

        tagDao.addTagToMemo(memoId1, tagId);
        tagDao.addTagToMemo(memoId2, tagId);

        List<Integer> memoIds = tagDao.findMemoIdsByTagId(tagId);
        assertTrue(memoIds.contains(memoId1), "メモ1が含まれているか");
        assertTrue(memoIds.contains(memoId2), "メモ2が含まれているか");
    }
}
