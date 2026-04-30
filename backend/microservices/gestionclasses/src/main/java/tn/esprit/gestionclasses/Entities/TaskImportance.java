package tn.esprit.gestionclasses.Entities;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Priorité d'affichage : plus le {@code sortOrder} est petit, plus la tâche est en haut.
 */
@Getter
@RequiredArgsConstructor
public enum TaskImportance {
    VERY_IMPORTANT(0),
    MEDIUM(1),
    COULD_WAIT(2);

    private final int sortOrder;
}
