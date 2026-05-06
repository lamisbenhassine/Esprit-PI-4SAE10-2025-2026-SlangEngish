package tn.esprit.gestioncours.Controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tn.esprit.gestioncours.DTO.RecordingRequestDto;
import tn.esprit.gestioncours.DTO.RecordingResponseDto;
import tn.esprit.gestioncours.Services.IRecordingService;

import java.util.List;

@RestController
@RequestMapping("/recording")
@Slf4j
public class RecordingController {

    private final IRecordingService recordingService;

    public RecordingController(@Qualifier("recordingServiceImpl") IRecordingService recordingService) {
        this.recordingService = recordingService;
    }

    @PostMapping("/create")
    public ResponseEntity<RecordingResponseDto> createRecording(@RequestBody RecordingRequestDto request) {
        RecordingResponseDto created = recordingService.createRecording(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<RecordingResponseDto> updateRecording(@PathVariable Long id,
                                                                @RequestBody RecordingRequestDto request) {
        RecordingResponseDto updated = recordingService.updateRecording(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteRecording(@PathVariable Long id) {
        boolean deleted = recordingService.deleteRecording(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<RecordingResponseDto> getAllRecordings() {
        return recordingService.getAllRecordings();
    }

    @GetMapping("/available")
    public List<RecordingResponseDto> getAvailableRecordings() {
        return recordingService.getAvailableRecordings();
    }

    @PostMapping("/upload/{id}")
    public ResponseEntity<RecordingResponseDto> uploadRecordingFile(@PathVariable Long id,
                                                                    @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            String rootDir = System.getProperty("user.dir");
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(rootDir, "uploads", "recordings");
            java.nio.file.Files.createDirectories(uploadDir);

            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            }

            String filename = "recording_" + id + "_" + System.currentTimeMillis() + extension;
            java.nio.file.Path destination = uploadDir.resolve(filename);
            java.nio.file.Files.copy(
                    file.getInputStream(),
                    destination,
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            String publicUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/media/recordings/")
                    .path(filename)
                    .toUriString();

            RecordingResponseDto updated = recordingService.attachFile(id, publicUrl);
            if (updated == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            log.error("Error while uploading recording file for id {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}

