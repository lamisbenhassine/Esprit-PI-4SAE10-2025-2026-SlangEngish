package org.example.evenement.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "inscriptions_evenement")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InscriptionEvenement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @Column(name = "date_inscription")
    private LocalDate dateInscription;

    @ManyToOne
    @JoinColumn(name = "evenement_id", nullable = false)
    @JsonBackReference
    private Evenement evenement;
}

