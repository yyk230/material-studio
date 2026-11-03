package com.example.studio.upload;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;

import com.example.studio.config.AppProperties;

/** 本地文件存储：uploads/ 用户素材，work/ 处理中间产物，results/ 最终结果。 */
@Service
public class StorageService {

    private final Path root;

    public StorageService(AppProperties props) {
        this.root = Path.of(props.storage().root()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root.resolve("uploads"));
            Files.createDirectories(root.resolve("work"));
            Files.createDirectories(root.resolve("results"));
        } catch (IOException e) {
            throw new UncheckedIOException("无法创建存储目录 " + root, e);
        }
    }

    /** 把相对路径解析为绝对路径，并拒绝越出存储根目录的路径。 */
    public Path resolve(String relativePath) {
        Path path = root.resolve(relativePath).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("非法存储路径: " + relativePath);
        }
        return path;
    }

    public String saveUpload(long userId, String fileName, InputStream content) throws IOException {
        String relative = "uploads/" + userId + "/" + fileName;
        Path target = resolve(relative);
        Files.createDirectories(target.getParent());
        Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        return relative;
    }

    public Path workDir(String taskId) {
        return resolve("work/" + taskId);
    }

    public Path resultFile(String taskId) {
        return resolve("results/" + taskId + ".svg");
    }

    public void deleteRecursively(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(dir)) {
            for (Path p : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(p);
            }
        }
    }
}
