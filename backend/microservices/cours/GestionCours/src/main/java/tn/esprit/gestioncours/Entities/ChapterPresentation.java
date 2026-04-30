package tn.esprit.gestioncours.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_presentation")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterPresentation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", referencedColumnName = "idChapter", unique = true, nullable = false)
    private Chapter chapter;

    @Lob
    @Column(name = "slides_json", columnDefinition = "LONGTEXT", nullable = false)
    private String slidesJson;

    /** Chemin absolu du fichier MP3 sur disque, ou null si pas de TTS. */
    @Column(name = "audio_path", length = 2048)
    private String audioPath;

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
