package tn.esprit.gestioncours.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(
        name = "progression",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "chapter_id_chapter"})
)
public class Progression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Identifiant fonctionnel de l'utilisateur (issu du service d'authentification).
     * On ne mappe pas ici une entité User car elle vit dans un autre microservice.
     */
    private Long userId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chapter_id_chapter")
    private Chapter chapter;

    private Integer lastPage;

    private Double progressPercentage;
}

