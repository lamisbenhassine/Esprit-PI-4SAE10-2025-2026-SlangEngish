package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestionclasses.Entities.KanbanColumn;
import tn.esprit.gestionclasses.Entities.TaskImportance;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentKanbanTaskRequestDto {

    private String title;
    private String description;
    private TaskImportance importance;
    /**
     * Colonne initiale à la création uniquement (TODO par défaut).
     * Pour déplacer une tâche, utiliser {@code PATCH /kanban/tasks/{id}/move}.
     */
    private KanbanColumn columnStatus;
}
