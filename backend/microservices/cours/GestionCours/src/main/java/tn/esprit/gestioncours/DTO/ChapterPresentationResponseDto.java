package tn.esprit.gestioncours.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChapterPresentationResponseDto {
    private Long chapterId;
    private List<PresentationSlideDto> slides;
    /** Indique si un fichier audio est disponible (URL construite côté client). */
    private boolean audioAvailable;
    private LocalDateTime updatedAt;
    private boolean fromCache;
}
