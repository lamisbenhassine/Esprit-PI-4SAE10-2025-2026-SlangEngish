package org.example.evenement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "liste_attente_evenement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ListeAttenteEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "date_ajout", nullable = false)
    private LocalDateTime dateAjout;

    @ManyToOne
    @JoinColumn(name = "evenement_id", nullable = false)
    @JsonBackReference
    private Evenement evenement;
}
