package org.example.club.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "posts_club")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Texte du post (titre + corps + hashtags) — LONGTEXT pour éviter la troncature MySQL (ex. VARCHAR 255). */
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String contenu;

    @Column(name = "date_post")
    private LocalDate datePost;

    @Column(name = "id_auteur", nullable = false)
    private Long idAuteur;

    /**
     * JSON array des médias (data URLs ou URLs): ["data:image/...","data:video/..."]
     */
    @Column(name = "medias", columnDefinition = "LONGTEXT")
    private String medias;

    @ManyToOne
    @JoinColumn(name = "club_id", nullable = false)
    private Club club;
}
