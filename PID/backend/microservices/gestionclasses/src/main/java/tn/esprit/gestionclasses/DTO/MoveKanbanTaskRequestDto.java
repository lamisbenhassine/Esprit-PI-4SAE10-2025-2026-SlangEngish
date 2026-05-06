package tn.esprit.gestionclasses.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tn.esprit.gestionclasses.Entities.KanbanColumn;

/**
 * Corps pour le drag-and-drop : colonne cible et index dans la liste <strong>déjà triée</strong>
 * (importance puis position), comme renvoyée par {@code GET /kanban/board}.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MoveKanbanTaskRequestDto {

    private KanbanColumn targetColumn;
    /** Index 0-based dans l'ordre d'affichage final de la colonne cible. */
    private int targetIndex;
}
