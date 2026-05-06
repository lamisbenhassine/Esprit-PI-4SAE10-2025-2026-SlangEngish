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
public class ChapterLearningContentResponseDto {
    private Long chapterId;
    private String summaryEnglish;
    private List<FlashcardItemDto> flashcards;
    private LocalDateTime updatedAt;
    /** true si issu du cache sans nouvel appel LLM */
    private boolean fromCache;
}
