package tn.esprit.gestionclasses.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "student_kanban_task")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StudentKanbanTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant de l'étudiant (même convention que les autres microservices).
     */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private KanbanColumn columnStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TaskImportance importance;

    /**
     * Ordre d'affichage dans la colonne, après tri par importance (0 = premier parmi les tâches de même importance).
     */
    @Column(nullable = false)
    private int positionInColumn;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
