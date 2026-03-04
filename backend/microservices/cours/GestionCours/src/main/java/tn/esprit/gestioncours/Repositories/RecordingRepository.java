package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.Recording;
import tn.esprit.gestioncours.Entities.RecordingStatus;

import java.util.List;

public interface RecordingRepository extends JpaRepository<Recording, Long> {

    List<Recording> findByStatus(RecordingStatus status);
}

