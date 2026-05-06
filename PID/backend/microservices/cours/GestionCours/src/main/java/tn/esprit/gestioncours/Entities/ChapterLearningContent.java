package tn.esprit.gestioncours.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "chapter_learning_content")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChapterLearningContent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", referencedColumnName = "idChapter", unique = true, nullable = false)
    private Chapter chapter;

    @Lob
    @Column(name = "summary_english", columnDefinition = "LONGTEXT")
    private String summaryEnglish;
    @Lob
    @Column(name = "flashcards_json", columnDefinition = "LONGTEXT", nullable = false)
    private String flashcardsJson;

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
