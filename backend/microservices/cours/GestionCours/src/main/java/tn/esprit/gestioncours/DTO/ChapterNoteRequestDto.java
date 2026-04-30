package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestioncours.Entities.NoteTag;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterNoteRequestDto {

    private String title;

    private String content;

    private NoteTag tag;
}
