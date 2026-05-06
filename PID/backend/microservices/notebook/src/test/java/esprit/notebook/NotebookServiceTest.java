package esprit.notebook;

import esprit.notebook.dto.DashboardResponse;
import esprit.notebook.dto.NoteDto;
import esprit.notebook.model.Note;
import esprit.notebook.repository.NoteRepository;
import esprit.notebook.service.NotebookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class NotebookServiceTest {

    private NoteRepository noteRepository;
    private NotebookService notebookService;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        notebookService = new NotebookService(noteRepository);
    }

    @Test
    void create_shouldPersistNoteWithComputedShareScore() {
        Long userId = 42L;
        String title = " My title ";
        String content = "Hello world hello notebook";

        Note saved = new Note();
        saved.setId(1L);
        saved.setUserId(userId);
        saved.setTitle("My title");
        saved.setContent(content);
        saved.setShareScore(123);

        when(noteRepository.save(any(Note.class))).thenReturn(saved);

        NoteDto dto = notebookService.create(userId, title, content);

        ArgumentCaptor<Note> captor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(captor.capture());

        Note toSave = captor.getValue();
        assertThat(toSave.getUserId()).isEqualTo(userId);
        assertThat(toSave.getTitle()).isEqualTo("My title");
        assertThat(toSave.getContent()).isEqualTo(content);
        // shareScore is computed internally; just assert it is non-null and non-negative
        assertThat(toSave.getShareScore()).isNotNull();
        assertThat(toSave.getShareScore()).isGreaterThanOrEqualTo(0);

        assertThat(dto.getId()).isEqualTo(1L);
        assertThat(dto.getTitle()).isEqualTo("My title");
    }

    @Test
    void update_shouldThrowForbiddenWhenUserMismatch() {
        Note existing = new Note();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setTitle("T1");
        existing.setContent("C1");

        when(noteRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> notebookService.update(999L, 10L, "New", "Body"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Forbidden");

        verify(noteRepository, never()).save(any());
    }

    @Test
    void listShared_shouldReturnDtosFromRepository() {
        Note n1 = new Note();
        n1.setId(1L);
        n1.setUserId(1L);
        n1.setTitle("A");
        n1.setContent("Alpha");
        n1.setShared(true);
        n1.setSharedAt(Instant.now());

        Note n2 = new Note();
        n2.setId(2L);
        n2.setUserId(2L);
        n2.setTitle("B");
        n2.setContent("Beta");
        n2.setShared(true);
        n2.setSharedAt(Instant.now());

        when(noteRepository.findBySharedTrueOrderBySharedAtDescUpdatedAtDesc())
                .thenReturn(List.of(n1, n2));

        List<NoteDto> dtos = notebookService.listShared(123L);

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(0).getId()).isEqualTo(1L);
        assertThat(dtos.get(1).getId()).isEqualTo(2L);
    }

    @Test
    void importShared_shouldCopyExistingSharedNote() {
        String shareId = "abc-123";
        Note src = new Note();
        src.setId(5L);
        src.setUserId(10L);
        src.setTitle("Lesson");
        src.setContent("Some content");
        src.setShared(true);
        src.setShareId(shareId);

        when(noteRepository.findByShareId(shareId)).thenReturn(Optional.of(src));

        Note copySaved = new Note();
        copySaved.setId(99L);
        copySaved.setUserId(7L);
        copySaved.setTitle("Lesson (copy)");
        copySaved.setContent("Some content");
        copySaved.setShared(false);
        copySaved.setShareScore(50);

        when(noteRepository.save(any(Note.class))).thenReturn(copySaved);

        NoteDto dto = notebookService.importShared(7L, shareId);

        assertThat(dto.getId()).isEqualTo(99L);
        assertThat(dto.getUserId()).isEqualTo(7L);
        assertThat(dto.getTitle()).isEqualTo("Lesson (copy)");
        assertThat(dto.isShared()).isFalse();
    }

    @Test
    void importShared_shouldFailWhenNoteNotShared() {
        String shareId = "abc-123";
        Note src = new Note();
        src.setId(5L);
        src.setUserId(10L);
        src.setTitle("Lesson");
        src.setContent("Some content");
        src.setShared(false);
        src.setShareId(shareId);

        when(noteRepository.findByShareId(shareId)).thenReturn(Optional.of(src));

        assertThatThrownBy(() -> notebookService.importShared(7L, shareId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Shared note not found");
    }

    @Test
    void dashboard_shouldReturnBestNoteAndTopWords() {
        Note n1 = new Note();
        n1.setId(1L);
        n1.setUserId(1L);
        n1.setContent("Hello world");
        n1.setShareScore(10);

        Note n2 = new Note();
        n2.setId(2L);
        n2.setUserId(1L);
        n2.setContent("Hello notebook notebook");
        n2.setShareScore(20);

        when(noteRepository.findByUserIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(n1, n2));

        DashboardResponse dashboard = notebookService.dashboard(1L);

        assertThat(dashboard).isNotNull();
        assertThat(dashboard.getBestNoteToShare()).isNotNull();
        assertThat(dashboard.getBestNoteToShare().getId()).isEqualTo(2L);
        assertThat(dashboard.getMostUsedWords()).isNotEmpty();
        assertThat(dashboard.getMostUsedWords().stream().map(DashboardResponse.WordCount::getWord))
                .contains("hello", "notebook", "world");
    }
}

