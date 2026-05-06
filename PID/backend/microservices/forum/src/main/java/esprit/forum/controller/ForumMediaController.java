package esprit.forum.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/forum/media")
@CrossOrigin(origins = "*")
public class ForumMediaController {

    private static final Set<String> ALLOWED_EXT = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".mp4", ".webm",
            ".mp3", ".ogg", ".wav", ".m4a", ".aac");

    @Value("${forum.upload-dir:forum-uploads}")
    private String uploadDir;

    /** Pas de `consumes` strict : le client envoie `multipart/form-data; boundary=…`, évite refus de matching. */
    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Fichier vide"));
        }
        String ext = resolveExtension(file);
        if (!ALLOWED_EXT.contains(ext)) {
            String ct = file.getContentType() != null ? file.getContentType() : "(inconnu)";
            String fn = file.getOriginalFilename() != null ? file.getOriginalFilename() : "(sans nom)";
            return ResponseEntity.badRequest().body(Map.of(
                    "error",
                    "Extension ou type MIME non pris en charge. Images, vidéo MP4/WebM ou audio MP3/OGG/WAV/WebM. "
                            + "Fichier: " + fn + " ; type: " + ct));
        }
        try {
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String name = UUID.randomUUID() + ext;
            Path target = dir.resolve(name).normalize();
            if (!target.startsWith(dir)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chemin invalide"));
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            return ResponseEntity.ok(Map.of("url", "/api/forum/media/files/" + name));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/files/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        if (filename.contains("..")) {
            return ResponseEntity.notFound().build();
        }
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Path file = dir.resolve(filename).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        String contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                contentType = probed;
            }
        } catch (IOException ignored) {
            // keep default
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    /** Nom de fichier (ex: .jpg) puis repli sur le Content-Type (photos sans extension, blob, etc.). */
    private static String resolveExtension(MultipartFile part) {
        String fromName = extension(part.getOriginalFilename());
        if (ALLOWED_EXT.contains(fromName)) {
            return normalizeExt(fromName);
        }
        String fromCt = extensionFromContentType(part.getContentType());
        if (ALLOWED_EXT.contains(fromCt)) {
            return normalizeExt(fromCt);
        }
        return "";
    }

    /** .jpeg → .jpg pour un nom de fichier cohérent */
    private static String normalizeExt(String ext) {
        if (".jpeg".equals(ext)) {
            return ".jpg";
        }
        return ext;
    }

    private static String extension(String name) {
        if (name == null || !name.contains(".")) {
            return "";
        }
        return name.substring(name.lastIndexOf('.')).toLowerCase();
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        String ct = contentType.toLowerCase().split(";")[0].trim();
        return switch (ct) {
            case "image/jpeg", "image/jpg", "image/pjpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "video/mp4" -> ".mp4";
            case "video/webm" -> ".webm";
            case "audio/webm" -> ".webm";
            case "audio/ogg", "application/ogg" -> ".ogg";
            case "audio/mpeg", "audio/mp3" -> ".mp3";
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/mp4", "audio/x-m4a" -> ".m4a";
            case "audio/aac" -> ".aac";
            default -> "";
        };
    }
}
