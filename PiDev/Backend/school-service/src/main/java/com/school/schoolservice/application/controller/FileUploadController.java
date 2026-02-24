package com.school.schoolservice.application.controller;

import java.io.IOException;
import java.nio.file.*;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
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
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path path = Paths.get(UPLOAD_DIR + filename);
        Files.write(path, file.getBytes());
        return ResponseEntity.ok("http://localhost:8081/api/files/" + filename);
    }

    @GetMapping("/{filename}")
    public ResponseEntity<byte[]> getFile(
            @PathVariable String filename) throws IOException {
        Path path = Paths.get(UPLOAD_DIR + filename);
        byte[] bytes = Files.readAllBytes(path);
        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .body(bytes);
    }
}