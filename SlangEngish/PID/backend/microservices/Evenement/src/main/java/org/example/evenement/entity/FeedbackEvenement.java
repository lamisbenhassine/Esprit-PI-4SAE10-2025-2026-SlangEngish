package org.example.evenement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_evenement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @Column(nullable = false)
    private Integer note;  // 1 à 5 étoiles

    @Column(length = 1000)
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @ManyToOne
    @JoinColumn(name = "evenement_id", nullable = false)
    @JsonBackReference
    private Evenement evenement;
}
