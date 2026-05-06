package tn.esprit.gestioncours.Controllers;

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
import org.springframework.web.bind.annotation.RestController;
import tn.esprit.gestioncours.DTO.StreamRequestDto;
import tn.esprit.gestioncours.DTO.StreamResponseDto;
import tn.esprit.gestioncours.Services.IStreamService;

import java.util.List;

@RestController
@RequestMapping("/stream")
public class StreamController {

    private final IStreamService streamService;

    public StreamController(@Qualifier("streamServiceImpl") IStreamService streamService) {
        this.streamService = streamService;
    }

    @PostMapping("/create")
    public ResponseEntity<StreamResponseDto> createStream(@RequestBody StreamRequestDto request) {
        StreamResponseDto created = streamService.createStream(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<StreamResponseDto> updateStream(@PathVariable Long id,
                                                          @RequestBody StreamRequestDto request) {
        StreamResponseDto updated = streamService.updateStream(id, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteStream(@PathVariable Long id) {
        boolean deleted = streamService.deleteStream(id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/all")
    public List<StreamResponseDto> getAllStreams() {
        return streamService.getAllStreams();
    }

    @GetMapping("/live")
    public List<StreamResponseDto> getLiveStreams() {
        return streamService.getLiveStreams();
    }
}
