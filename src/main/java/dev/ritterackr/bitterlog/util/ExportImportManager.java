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
}