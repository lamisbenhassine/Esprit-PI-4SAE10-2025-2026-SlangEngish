package org.example.evenement.repository;

import org.example.evenement.entity.FeedbackEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackEvenementRepository extends JpaRepository<FeedbackEvenement, Long> {

    List<FeedbackEvenement> findByEvenementId(Long evenementId);

    Optional<FeedbackEvenement> findByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);

    boolean existsByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);

    @Query("SELECT AVG(f.note) FROM FeedbackEvenement f WHERE f.evenement.id = :evenementId")
    Double getMoyenneByEvenementId(@Param("evenementId") Long evenementId);

    long countByEvenementId(Long evenementId);
}
