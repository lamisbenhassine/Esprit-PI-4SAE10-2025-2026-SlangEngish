package tn.esprit.gestioncours.Services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tn.esprit.gestioncours.DTO.ChapterNoteRequestDto;
import tn.esprit.gestioncours.DTO.ChapterNoteResponseDto;
import tn.esprit.gestioncours.Entities.Chapter;
import tn.esprit.gestioncours.Entities.ChapterNote;
import tn.esprit.gestioncours.Entities.NoteTag;
import tn.esprit.gestioncours.Repositories.ChapterNoteRepository;
import tn.esprit.gestioncours.Repositories.ChapterRepository;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChapterNoteServiceImpl implements IChapterNoteService {

    private final ChapterNoteRepository chapterNoteRepository;
    private final ChapterRepository chapterRepository;

    @Override
    public ChapterNoteResponseDto createNote(Long userId, Long chapterId, ChapterNoteRequestDto request) {
        Chapter chapter = chapterRepository.findById(chapterId).orElse(null);
        if (chapter == null) {
            return null;
        }
        ChapterNote note = new ChapterNote();
        note.setUserId(userId);
        note.setChapter(chapter);
        applyRequest(request, note);
        if (note.getTag() == null) {
            note.setTag(NoteTag.GRAMMAR);
        }
        ChapterNote saved = chapterNoteRepository.save(note);
        return mapToDto(saved);
    }

    @Override
    public ChapterNoteResponseDto updateNote(Long id, Long userId, ChapterNoteRequestDto request) {
        return chapterNoteRepository.findByIdAndUserId(id, userId)
                .map(existing -> {
                    applyRequest(request, existing);
                    ChapterNote updated = chapterNoteRepository.save(existing);
                    return mapToDto(updated);
                })
                .orElse(null);
    }

    @Override
    public boolean deleteNote(Long id, Long userId) {
        return chapterNoteRepository.findByIdAndUserId(id, userId)
                .map(note -> {
                    chapterNoteRepository.delete(note);
                    return true;
                })
                .orElse(false);
    }

    @Override
    public List<ChapterNoteResponseDto> listNotesForChapter(Long userId, Long chapterId, String search) {
        if (!chapterRepository.existsById(chapterId)) {
            return List.of();
        }
        String q = search == null ? "" : search.trim();
        int blankFlag = q.isEmpty() ? 1 : 0;
        String pattern = blankFlag == 1 ? "%" : "%" + q.toLowerCase(Locale.ROOT) + "%";
        return chapterNoteRepository
                .findByUserAndChapterWithOptionalSearch(userId, chapterId, blankFlag, pattern)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private void applyRequest(ChapterNoteRequestDto request, ChapterNote note) {
        if (request.getTitle() != null) {
            note.setTitle(request.getTitle());
        }
        if (request.getContent() != null) {
            note.setContent(request.getContent());
        }
        if (request.getTag() != null) {
            note.setTag(request.getTag());
        }
    }

    private ChapterNoteResponseDto mapToDto(ChapterNote note) {
        return new ChapterNoteResponseDto(
                note.getId(),
                note.getChapter().getIdChapter(),
                note.getTitle(),
                note.getContent(),
                note.getTag(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
