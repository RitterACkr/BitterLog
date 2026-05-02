package dev.ritterackr.bitterlog.util;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.UUID;

/**
 * 画像の保存・圧縮・リサイズを管理するユーティリティクラス
 */
public class ImageManager {

    /** 画像保存ディレクトリ */
    private static final String IMAGE_DIR = System.getProperty("user.home") + "/BitterLog/images/";

    /** 画像の最大幅・高さ (px) */
    private static final int MAX_SIZE = 1280;

    /** JPEG圧縮品質 (0.0 - 1.0 */
    private static final float JPEG_QUALITY = 0.85f;

    /**
     * 画像保存ディレクトリの初期化
     */
    public static void initDirectory() {
        Path dir = Paths.get(IMAGE_DIR);
        dir.toFile().mkdirs();
    }

    /**
     * 画像ファイルを保存<br>
     * 最大サイズを超える場合はリサイズし, JPEGは圧縮
     * @param sourceFile 保存元の画像ファイル
     * @return 保存先のファイルパス
     * @throws IOException 保存失敗時のスタックトレース
     */
    public static String saveImage(File sourceFile) throws IOException {
        initDirectory();

        String fileName = sourceFile.getName();
        String extension = getExtension(fileName).toLowerCase();

        // GIFはコピー
        if (extension.equals("gif")) return copyAsIs(sourceFile);

        BufferedImage image = ImageIO.read(sourceFile);
        if (image == null) throw new IOException("Failed to load image: " + fileName);

        // リサイズが必要か確認
        image = resizeIfNeeded(image);

        // 保存フォーマット
        String saveExtension = extension.equals("jpg") || extension.equals("jpeg") ? "jpg" : "png";
        String saveName = UUID.randomUUID().toString() + "." + saveExtension;
        File destFile = new File(IMAGE_DIR + saveName);

        // 保存
        if (saveExtension.equals("jpg")) {
            saveAsJpeg(image, destFile);
        } else {
            ImageIO.write(image, "png", destFile);
        }

        return destFile.getAbsolutePath();
    }

    /**
     * クリップぼおーどの画像を保存する
     * @param image クリップボードから取得した BufferedImage
     * @return 保存先のファイルパス
     * @throws IOException 保存失敗時のスタックトレース
     */
    public static String saveImageFromClipboard(BufferedImage image) throws IOException {
        initDirectory();

        image = resizeIfNeeded(image);
        String saveName = UUID.randomUUID().toString() + ".png";
        File destFile = new File(IMAGE_DIR + saveName);
        ImageIO.write(image, "png", destFile);

        return destFile.getAbsolutePath();
    }

    /**
     * 画像を最大サイズに収まるようにリサイズする
     * @param image リサイズ対象の画像
     * @return リサイズ後の画像
     */
    private static BufferedImage resizeIfNeeded(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= MAX_SIZE && height <= MAX_SIZE) return image;

        // アスペクト比を保ってリサイズ
        double scale = Math.min((double) MAX_SIZE / width, (double) MAX_SIZE / height);
        int newWidth = (int) (width * scale);
        int newHeight = (int) (height * scale);

        BufferedImage resized = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(image, 0, 0, newWidth, newHeight, null);
        g.dispose();

        return resized;
    }

    /**
     * JPEG形式で画像を保存
     * @param image 保存する画像
     * @param destFile 保存先ファイル
     * @throws IOException 保存失敗時のスタックトレース
     */
    private static void saveAsJpeg(BufferedImage image, File destFile) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
        if (!writers.hasNext()) throw new IOException("JPEG Writer was not found.");

        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(JPEG_QUALITY);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(destFile)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    /**
     * ファイルをそのままコピーして保存する
     * @param sourceFile コピー元ファイル
     * @return 保存先のファイルパス
     * @throws IOException コピー失敗時のスタックトレース
     */
    private static String copyAsIs(File sourceFile) throws IOException {
        String saveName = UUID.randomUUID().toString() + "." + getExtension(sourceFile);
        Path destPath = Paths.get(IMAGE_DIR + saveName);
        Files.copy(sourceFile.toPath(), destPath);
        return destPath.toAbsolutePath().toString();
    }

    /**
     * ファイル名から拡張子を取得
     * @param fileName ファイル名
     * @return 拡張子
     */
    private static String getExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(dotIndex + 1) : "png";
    }

    /**
     * 画像ディレクトリのパスを返す
     * @return 画像ディレクトリのパス
     */
    public static String getImageDir() {
        return IMAGE_DIR;
    }
}
