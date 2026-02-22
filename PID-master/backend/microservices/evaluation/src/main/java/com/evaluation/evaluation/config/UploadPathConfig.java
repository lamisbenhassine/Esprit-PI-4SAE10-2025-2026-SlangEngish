package com.evaluation.evaluation.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the upload directory so both upload and generate-from-pdf use the same path
 * (avoids 500 when run from different working directories, e.g. project root vs module).
 */
@Component
public class UploadPathConfig {

    @Value("${app.upload.dir:uploads}")
    private String uploadDirConfig;

    private Path resolvedUploadDir;

    @PostConstruct
    public void init() {
        Path configured = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
        Path fallback = Paths.get(System.getProperty("user.dir", ".")).resolve("frontend").resolve("uploads");
        if (Files.exists(fallback)) {
            resolvedUploadDir = fallback;
        } else if (Files.exists(configured) || (configured.getParent() != null && Files.exists(configured.getParent()))) {
            resolvedUploadDir = configured;
        } else {
            resolvedUploadDir = fallback;
        }
    }

    public Path getUploadDir() {
        return resolvedUploadDir;
    }

    public Path resolve(String filename) {
        return resolvedUploadDir.resolve(filename);
    }
}
