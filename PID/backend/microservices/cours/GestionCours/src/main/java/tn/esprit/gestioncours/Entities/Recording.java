package tn.esprit.gestioncours.Entities;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Recording {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    /**
     * Lien du live original (meeting Jitsi).
     */
    @Column(length = 1024)
    private String streamLink;

    /**
     * Lien vers la vidéo enregistrée (hébergement externe, storage, etc.).
     */
    @Column(length = 1024)
    private String recordingLink;

    private LocalDateTime recordedAt;

    @Enumerated(EnumType.STRING)
    private RecordingStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecordingAnalysisStatus analysisStatus = RecordingAnalysisStatus.PENDING;

    @OneToMany(mappedBy = "recording", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecordingHighlight> highlights = new ArrayList<>();
}

