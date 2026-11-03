package com.example.studio.upload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

/** 按文件头魔数识别图片真实类型，不信任客户端声明的 Content-Type。 */
public final class FileTypeDetector {

    public static final String JPEG = "image/jpeg";
    public static final String PNG = "image/png";
    public static final String WEBP = "image/webp";

    private static final Map<String, String> EXTENSIONS = Map.of(JPEG, "jpg", PNG, "png", WEBP, "webp");
    private static final int HEADER_LENGTH = 12;

    private FileTypeDetector() {
    }

    public static Optional<String> detect(byte[] head) {
        if (startsWith(head, 0xFF, 0xD8, 0xFF)) {
            return Optional.of(JPEG);
        }
        if (startsWith(head, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return Optional.of(PNG);
        }
        if (head.length >= HEADER_LENGTH
                && startsWith(head, 'R', 'I', 'F', 'F')
                && head[8] == 'W' && head[9] == 'E' && head[10] == 'B' && head[11] == 'P') {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    public static Optional<String> detect(InputStream in) throws IOException {
        return detect(in.readNBytes(HEADER_LENGTH));
    }

    public static Optional<String> detect(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            return detect(in);
        }
    }

    public static String extension(String mimeType) {
        return EXTENSIONS.get(mimeType);
    }

    /** 用于错误提示，例如 image/jpeg,image/png → JPG/PNG。 */
    public static String displayName(String mimeType) {
        String ext = extension(mimeType);
        return ext == null ? mimeType : ext.toUpperCase();
    }

    private static boolean startsWith(byte[] data, int... prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if ((data[i] & 0xFF) != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
