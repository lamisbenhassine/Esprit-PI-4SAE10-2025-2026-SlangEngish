package org.example.club.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_club")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_etudiant", nullable = false)
    private Long idEtudiant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_club", nullable = false)
    private Club club;

    @Column(nullable = false)
    private Integer note; // 1..5

    @Column(columnDefinition = "TEXT")
    private String commentaire;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation = LocalDateTime.now();
}

