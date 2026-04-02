package org.example.club.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "reunions_club")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReunionClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    private LocalTime heure;
    private String lieu;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type_reunion")
    private TypeReunion typeReunion = TypeReunion.PRESENTIEL;

    @Column(name = "lien_reunion")
    private String lienReunion;

    @Enumerated(EnumType.STRING)
    @Column(name = "audience")
    private ReunionAudience audience = ReunionAudience.TOUS_CLUB;

    /**
     * Départements ciblés (CSV) si audience = DEPARTEMENTS
     */
    @Column(name = "departements_cibles", columnDefinition = "TEXT")
    private String departementsCibles;

    @ManyToOne
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
}
