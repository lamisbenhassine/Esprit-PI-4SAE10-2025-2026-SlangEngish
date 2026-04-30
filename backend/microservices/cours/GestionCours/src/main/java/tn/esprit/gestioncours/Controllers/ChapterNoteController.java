package tn.esprit.gestioncours.Controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.gestioncours.DTO.ChapterNoteRequestDto;
import tn.esprit.gestioncours.DTO.ChapterNoteResponseDto;
import tn.esprit.gestioncours.Services.IChapterNoteService;

import java.util.List;

@RestController
@RequestMapping("/chapter-note")
@Slf4j
public class ChapterNoteController {

    private final IChapterNoteService chapterNoteService;

    public ChapterNoteController(@Qualifier("chapterNoteServiceImpl") IChapterNoteService chapterNoteService) {
        this.chapterNoteService = chapterNoteService;
    }

    @PostMapping("/add/{chapterId}")
    public ResponseEntity<ChapterNoteResponseDto> addNote(
            @PathVariable Long chapterId,
            @RequestParam Long userId,
            @RequestBody ChapterNoteRequestDto request) {
        ChapterNoteResponseDto created = chapterNoteService.createNote(userId, chapterId, request);
        if (created == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<ChapterNoteResponseDto> updateNote(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestBody ChapterNoteRequestDto request) {
        ChapterNoteResponseDto updated = chapterNoteService.updateNote(id, userId, request);
        if (updated == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long id, @RequestParam Long userId) {
        boolean deleted = chapterNoteService.deleteNote(id, userId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/by-chapter/{chapterId}")
    public List<ChapterNoteResponseDto> listByChapter(
            @PathVariable Long chapterId,
            @RequestParam Long userId,
            @RequestParam(required = false) String search) {
        return chapterNoteService.listNotesForChapter(userId, chapterId, search);
    }
}
