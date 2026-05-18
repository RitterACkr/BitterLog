package dev.ritterackr.bitterlog.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;

import java.util.Arrays;

/**
 * Markdown を HTML に変換するユーティリティクラス<br>
 * Highlight.js によるシンタックスハイライトに対応した HTML を生成
 */
public class MarkdownRenderer {

    private static final Parser parser;
    private static final HtmlRenderer renderer;

    private static final String HIGHLIGHT_JS;
    private static final String HIGHLIGHT_CSS;

    static {
        // テーブル・打ち消し線などの拡張機能を有効化
        MutableDataSet options = new MutableDataSet();
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                StrikethroughExtension.create()
        ));

        parser = Parser.builder(options).build();
        renderer = HtmlRenderer.builder(options).build();

        String hlJs = "";
        String hlCss = "";
        try {
            var jsStream = MarkdownRenderer.class.getResourceAsStream("/dev/ritterackr/bitterlog/highlight.min.js");
            var cssStream = MarkdownRenderer.class.getResourceAsStream("/dev/ritterackr/bitterlog/github.min.css");
            if (jsStream != null) hlJs = new String(jsStream.readAllBytes());
            if (cssStream != null) hlCss = new String(cssStream.readAllBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
        HIGHLIGHT_JS = hlJs;
        HIGHLIGHT_CSS = hlCss;
    }

    /**
     * Markdown テキストを HTML に変換する<br>
     * Highlight.js と CSS を含む完全な HTML ページとして返す<br>
     * ローカル画像パス
     * @param markdown Markdownテキスト
     * @param isDark ダークモードかどうか
     * @return HTML文字列
     */
    public static String render(String markdown, boolean isDark) {
        markdown = convertLocalImagesToBase64(markdown);
        markdown = convertMemoLinks(markdown);
        Node document = parser.parse(markdown);
        String body = renderer.render(document);
        return wrapWithTemplate(body, isDark);
    }
    public static String render(String markdown) {
        return render(markdown, false);
    }

    /**
     * Markdown内のローカル画像パスをBase64データURIに変換する
     * @param markdown Markdownテキスト
     * @return 変換後のMarkdownテキスト
     */
    private static String convertLocalImagesToBase64(String markdown) {
        // ![alt](file:///path/to/image) 形式の検出
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "!\\[([^\\]]*)\\]\\(file:///([^)]+)\\)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(markdown);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String alt = matcher.group(1);
            String path = matcher.group(2);
            try {
                java.io.File file = new java.io.File(path);
                if (file.exists()) {
                    byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                    String base64 = java.util.Base64.getEncoder().encodeToString(bytes);
                    String ext = path.substring(path.lastIndexOf('.') + 1).toLowerCase();
                    String mimeType = ext.equals("jpg") || ext.equals("jpeg") ? "image/jpeg" : "image/" + ext;
                    String dataUri = "data:" + mimeType + ";base64," + base64;
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement("![" + alt + "](" + dataUri + ")"));
                } else {
                    matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(matcher.group(0)));
                }
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group(0));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * [[メモタイトル]] / [[メモタイトル#行番号]]
     */
    private static String convertMemoLinks(String markdown) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "\\[\\[([^\\]#]+)(?:#(\\d+))?\\]\\]"
        );
        java.util.regex.Matcher matcher = pattern.matcher(markdown);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String line = matcher.group(2);
            String linkText = line != null ? title + " #" + line : title;
            String href = "memo://" + title + (line != null ? "#" + line : "");
            String replacement = "[" + linkText + "](" + href + ")";
            matcher.appendReplacement(sb, java.util.regex.Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 変換された HTML をテンプレートで包む<br>
     * Highlight.js によるシンタックスハイライトと CSS を含む
     * @param body 変換されたHTML本文
     * @param isDark ダークモードかどうか
     * @return 完全なHTML文字列
     */
    private static String wrapWithTemplate(String body, boolean isDark) {
        String bodyClass = isDark ? " class=\"dark\"" : "";
        return ("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<style>" +
                HIGHLIGHT_CSS +
                "</style>\n<script>" +
                HIGHLIGHT_JS +
                "</script>\n<style>\n" +
                """
                        body {
                            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
                            font-size: 14px;
                            line-height: 1.6;
                            padding: 16px;
                            color: #24292e;
                        }
                        pre {
                            background-color: #f6f8fa !important;
                            border-radius: 6px;
                            padding: 12px;
                            overflow-x: auto;
                        }
                        code {
                            font-family: 'Consolas', 'Monaco', monospace;
                            font-size: 13px;
                        }
                        table {
                            border-collapse: collapse;
                            width: 100%;
                        }
                        th, td {
                            border: 1px solid #dfe2e5;
                            padding: 6px 12px;
                        }
                        th {
                            background-color: #f6f8fa;
                        }
                        body.dark {
                            background-color: #1e1e1e;
                            color: #ffffff;
                        }
                        body.dark pre {
                            background-color: #2d2d2d !important;
                            color: #e8d5c0;
                        }
                        body.dark .hljs {
                            background-color: #2d2d2d !important;
                        }
                        body.dark code {
                            color: #e8d5c0;
                        }
                        body.dark table {
                            color: #ffffff;
                        }
                        body.dark th {
                            background-color: #2d2d2d;
                        }
                        body.dark th, body.dark td {
                            border-color: #3d3d3d;
                        }
                    </style>
                    <script>
                        document.addEventListener('DOMContentLoaded', function() {
                            hljs.highlightAll();
                            document.querySelectorAll('pre code').forEach(function(block) {
                                var button = document.createElement('button');
                                button.textContent = 'Copy';
                                button.style.cssText =
                                    'position: absolute; top: 8px; right: 8px; padding: 2px 8px; ' +
                                    'font-size: 12px; cursor: pointer; border: 1px solid #ccc; ' +
                                    'border-radius: 4px; background: #fff;';
                                var pre = block.parentNode;
                                pre.style.position = 'relative';
                                pre.appendChild(button);
                                button.addEventListener('click', function() {
                                    javabridge.copyToClipboard(block.textContent);
                                    button.textContent = 'Copied!';
                                    setTimeout(function() {
                                        button.textContent = 'Copy';
                                    }, 2000);
                                });
                            });
                        });
                        document.addEventListener('click', function(e) {
                            var target = e.target;
                            if (target.tagName === 'A' && target.href.startsWith('memo://')) {
                                e.preventDefault();
                                var href = target.href.replace('memo://', '');
                                var parts = href.split('#');
                                var title = decodeURIComponent(parts[0]);
                                var line = parts.length > 1 ? parts[1] : '';
                                javabridge.openMemoLink(title, line);
                            }
                        });
                    </script>
                </head>
                <body""") + bodyClass + ">\n" + body + "</body>\n</html>";
    }
}
