package tn.esprit.gestioncours.Services;

import tn.esprit.gestioncours.DTO.ChapterNoteRequestDto;
import tn.esprit.gestioncours.DTO.ChapterNoteResponseDto;

import java.util.List;

public interface IChapterNoteService {

    ChapterNoteResponseDto createNote(Long userId, Long chapterId, ChapterNoteRequestDto request);

    ChapterNoteResponseDto updateNote(Long id, Long userId, ChapterNoteRequestDto request);

    boolean deleteNote(Long id, Long userId);

    List<ChapterNoteResponseDto> listNotesForChapter(Long userId, Long chapterId, String search);
}
