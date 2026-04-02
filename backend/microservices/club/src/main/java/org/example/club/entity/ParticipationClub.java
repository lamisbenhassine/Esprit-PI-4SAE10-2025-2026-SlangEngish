package org.example.club.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "participation_club")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_club", nullable = false)
    private Club club;

    @Column(name = "date_demande", nullable = false)
    private LocalDateTime dateDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutParticipation statut = StatutParticipation.PENDING;

    /**
     * Score calculé à partir des réponses numériques du formulaire (0-100)
     */
    @Column(name = "score")
    private Double score;

    /**
     * Texte de motivation saisi par l'étudiant
     */
    @Column(name = "texte_motivation", columnDefinition = "TEXT")
    private String texteMotivation;

    /**
     * Réponses du formulaire au format JSON (optionnel)
     */
    @Column(name = "reponses_formulaire", columnDefinition = "TEXT")
    private String reponsesFormulaire;

    @Column(name = "departement_souhaite")
    private String departementSouhaite;

    @Column(name = "departement_assigne")
    private String departementAssigne;
}
