package tn.esprit.gestioncours.Repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.gestioncours.Entities.Stream;
import tn.esprit.gestioncours.Entities.StreamStatus;

import java.util.List;

public interface StreamRepository extends JpaRepository<Stream, Long> {

    List<Stream> findByStatus(StreamStatus status);
}
