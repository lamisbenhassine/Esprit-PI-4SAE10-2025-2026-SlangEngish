package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecordingHighlightDto {
    private Long id;
    private Long recordingId;
    private Integer seconds;
    private String title;
    private String description;
    private LocalDateTime createdAt;
}
