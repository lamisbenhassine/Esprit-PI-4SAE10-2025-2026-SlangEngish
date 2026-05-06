package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.RecordingHighlight;

import java.util.List;

public interface RecordingHighlightRepository extends JpaRepository<RecordingHighlight, Long> {
    List<RecordingHighlight> findByRecording_IdOrderBySecondsAsc(Long recordingId);

    void deleteByRecording_Id(Long recordingId);
}
