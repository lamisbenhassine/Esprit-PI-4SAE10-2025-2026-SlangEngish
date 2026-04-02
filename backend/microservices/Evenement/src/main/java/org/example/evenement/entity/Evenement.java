package org.example.evenement.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "evenements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Evenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String titre;

    private String description;
    private String type;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime heure;
    private String lieu;
    private Integer capacite;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String image;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private EventStatus status;

    /**
     * Si l'événement est organisé par un club (sinon null pour un événement général / admin).
     */
    @Column(name = "id_club")
    private Long idClub;

    /**
     * Nom du club organisateur (dénormalisé pour l'affichage liste).
     */
    @Column(name = "nom_club_organisateur", length = 255)
    private String nomClubOrganisateur;

    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<InscriptionEvenement> inscriptions;

    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<ListeAttenteEvenement> listeAttente;

    /**
     * Avis sur l'événement (côté parent pour Jackson : {@link FeedbackEvenement#evenement} est en @JsonBackReference).
     */
    @OneToMany(mappedBy = "evenement", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<FeedbackEvenement> feedbacks;
}
