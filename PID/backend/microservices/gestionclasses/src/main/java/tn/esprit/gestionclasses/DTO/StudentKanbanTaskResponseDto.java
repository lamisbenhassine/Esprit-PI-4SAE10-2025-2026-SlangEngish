package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import tn.esprit.gestionclasses.Entities.KanbanColumn;
import tn.esprit.gestionclasses.Entities.TaskImportance;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentKanbanTaskResponseDto {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private KanbanColumn columnStatus;
    private TaskImportance importance;
    private int positionInColumn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
