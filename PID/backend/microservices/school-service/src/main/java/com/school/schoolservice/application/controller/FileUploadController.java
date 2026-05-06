package com.school.schoolservice.application.controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileUploadController {

    private static final String UPLOAD_DIR = "uploads/";

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        Files.createDirectories(Paths.get(UPLOAD_DIR));
        String originalName = file.getOriginalFilename() == null
                ? "file.pdf"
                : file.getOriginalFilename();
        String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String filename = UUID.randomUUID() + "_" + safeName;
        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, file.getBytes());
        // ✅ URL propre sans saut de ligne
        String url = "http://localhost:8081/api/files/" + filename;
        return ResponseEntity.ok(url.trim());
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(
            @PathVariable String filename) throws IOException {
        // ✅ Nettoie le filename
        String cleanFilename = filename.trim()
                .replaceAll("[\\r\\n]", "")
                .replaceAll("%0A", "");

        Path path = Paths.get(UPLOAD_DIR + cleanFilename);

        if (!Files.exists(path)) {
            System.out.println("❌ Fichier non trouvé : " + path.toAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        byte[] bytes = Files.readAllBytes(path);

        // ✅ Détecte le type de fichier
        String contentType = cleanFilename.endsWith(".pdf")
                ? "application/pdf"
                : "application/octet-stream";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + cleanFilename + "\"")
                .body(bytes);
    }
}