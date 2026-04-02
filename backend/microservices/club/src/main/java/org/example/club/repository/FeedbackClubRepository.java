package org.example.club.repository;

import org.example.club.entity.FeedbackClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FeedbackClubRepository extends JpaRepository<FeedbackClub, Long> {
    List<FeedbackClub> findByClubIdOrderByDateCreationDesc(Long clubId);
    Optional<FeedbackClub> findByIdEtudiantAndClubId(Long idEtudiant, Long clubId);

    @Query("SELECT f.club.id, AVG(f.note), COUNT(f) FROM FeedbackClub f GROUP BY f.club.id")
    List<Object[]> summarizeByClub();
}

