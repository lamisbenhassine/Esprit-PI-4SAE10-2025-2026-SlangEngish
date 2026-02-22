package com.evaluation.evaluation.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/evaluations")
public class ImageUploadController {

    private static final String[] ALLOWED_EXTENSIONS = { ".jpg", ".jpeg", ".png", ".gif", ".webp" };

    @Value("${server.port:8020}")
    private String serverPort;

    @Value("${app.upload.dir:uploads}")
    private String uploadDirConfig;

    @PostMapping("/upload-image")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No file provided"));
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid file name"));
        }
        String ext = getExtension(originalName);
        if (!isAllowedExtension(ext)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Allowed types: JPG, PNG, GIF, WEBP"));
        }
        try {
            Path uploadDir = Paths.get(uploadDirConfig).toAbsolutePath().normalize();
            Files.createDirectories(uploadDir);
            String savedName = UUID.randomUUID() + ext;
            Path target = uploadDir.resolve(savedName);
            Files.copy(file.getInputStream(), target);

            String url = "http://localhost:" + serverPort + "/uploads/" + savedName;
            Map<String, String> body = new HashMap<>();
            body.put("url", url);
            return ResponseEntity.ok(body);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save file: " + e.getMessage()));
        }
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        return i > 0 ? filename.substring(i).toLowerCase() : "";
    }

    private static boolean isAllowedExtension(String ext) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equals(ext)) return true;
        }
        return false;
    }
}
