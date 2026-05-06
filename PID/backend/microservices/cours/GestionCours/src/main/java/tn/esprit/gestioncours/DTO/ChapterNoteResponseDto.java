package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.NoteTag;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterNoteResponseDto {

    private Long id;

    private Long chapterId;

    private String title;

    private String content;

    private NoteTag tag;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
