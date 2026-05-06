package dev.ritterackr.bitterlog.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.ritterackr.bitterlog.dao.ImageDao;
import dev.ritterackr.bitterlog.dao.MemoDao;
import dev.ritterackr.bitterlog.dao.TagDao;
import dev.ritterackr.bitterlog.model.Image;
import dev.ritterackr.bitterlog.model.Memo;
import dev.ritterackr.bitterlog.model.Tag;

import java.io.*;
import java.nio.file.*;
import java.sql.SQLException;
import java.util.*;
import java.util.zip.*;

/**
 * データのエクスポート・インポートを管理するクラス<br>
 * ZIPファイル形式でメモデータと画像をまとめて扱う
 */
public class ExportImportManager {

    private static final MemoDao memoDao = new MemoDao();
    private static final TagDao tagDao = new TagDao();
    private static final ImageDao imageDao = new ImageDao();

    /**
     * 全データをZIPファイルにエクスポートする
     * @param zipFilePath エクスポート先のZIPファイルパス
     * @throws Exception エクスポート失敗時のスタックトレース
     */
    public static void export(String zipFilePath) throws Exception {
        List<Memo> memos = memoDao.findAll();
        List<Map<String, Object>> memoDataList = new ArrayList<>();

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
            for (Memo memo : memos) {
                // タグを取得
                List<Tag> tags = tagDao.findByMemoId(memo.getId());
                List<String> tagNames = tags.stream()
                    .map(Tag::getName)
                    .toList();

                // 画像を取得して追加
                List<Image> images = imageDao.findByMemoId(memo.getId());
                List<String> imageFileNames = new ArrayList<>();
                for (Image image : images) {
                    File imageFile = new File(image.getFilePath());
                    if (imageFile.exists()) {
                        String entryName = "images/" + imageFile.getName();
                        zos.putNextEntry(new ZipEntry(entryName));
                        Files.copy(imageFile.toPath(), zos);
                        zos.closeEntry();
                        imageFileNames.add(imageFile.getName());
                    }
                }

                // メモデータをMapに変換
                Map<String, Object> memoData = new LinkedHashMap<>();
                memoData.put("id", memo.getId());
                memoData.put("title", memo.getTitle());
                memoData.put("content", memo.getContent());
                memoData.put("categoryId", memo.getCategoryId());
                memoData.put("isPinned", memo.isPinned());
                memoData.put("isFavorite", memo.isFavorite());
                memoData.put("createdAt", memo.getCreatedAt().toString());
                memoData.put("updatedAt", memo.getUpdatedAt().toString());
                memoData.put("tags", tagNames);
                memoData.put("images", imageFileNames);
                memoDataList.add(memoData);
            }

            // メモデータをJSONとしてZIPに追加
            zos.putNextEntry(new ZipEntry("memos.json"));
            ObjectMapper mapper = new ObjectMapper();
            byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsBytes(memoDataList);
            zos.write(jsonBytes);
            zos.closeEntry();
        }
    }

    /**
     * ZIPファイルからデータをインポートする<br>
     * 同タイトルが存在する場合はスキップ
     * @param zipFilePath インポート元のZIPファイルパス
     * @throws Exception インポート失敗時のスタックトレース
     */
    public static void importData(String zipFilePath) throws Exception {
        // 画像の一時保存先を準備
        String imageDir = ImageManager.getImageDir();
        ImageManager.initDirectory();

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFilePath))) {
            ZipEntry entry;
            byte[] jsonBytes = null;
            Map<String, byte[]> imageDataMap = new HashMap<>();

            // ZIPの中身を読み込む
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().equals("memos.json")) {
                    jsonBytes = zis.readAllBytes();
                } else if (entry.getName().startsWith("images/")) {
                    String fileName = entry.getName().substring("images/".length());
                    imageDataMap.put(fileName, zis.readAllBytes());
                }
                zis.closeEntry();
            }

            if (jsonBytes == null) throw new Exception("memos.json was not found.");

            // JSONを解析
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> memoDataList = mapper.readValue(jsonBytes, List.class);

            for (Map<String, Object> memoData : memoDataList) {
                String title = (String) memoData.get("title");

                // 重複チェック
                List<Memo> existing = memoDao.search(title);
                boolean isDuplicate = existing.stream()
                    .anyMatch(m -> m.getTitle().equals(title));
                if (isDuplicate) continue;

                // メモを作成
                Memo memo = new Memo(title, (String) memoData.get("content"));
                memo.setPinned((Boolean) memoData.getOrDefault("isPinned", false));
                memo.setFavorite((boolean) memoData.getOrDefault("isFavorite", false));
                int memoId = memoDao.create(memo);

                // タグを設定
                List<String> tagNames = (List<String>) memoData.get("tags");
                if (tagNames != null) {
                    for (String tagName : tagNames) {
                        int tagId = tagDao.create(tagName);
                        tagDao.addTagToMemo(memoId, tagId);
                    }
                }

                // 画像を保存
                List<String> imageFileNames = (List<String>) memoData.get("tags");
                if (imageFileNames != null) {
                    for (String fileName : imageFileNames) {
                        byte[] imageData = imageDataMap.get(fileName);
                        if (imageData != null) {
                            String savePath = imageDir + fileName;
                            Files.write(Paths.get(savePath), imageData);

                            Image image = new Image(memoId, fileName, savePath);
                            imageDao.create(image);
                        }
                    }
                }
            }
        }
    }
}