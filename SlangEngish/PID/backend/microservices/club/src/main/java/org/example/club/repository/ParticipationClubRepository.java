package org.example.club.repository;

import org.example.club.entity.ParticipationClub;
import org.example.club.entity.StatutParticipation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ParticipationClubRepository extends JpaRepository<ParticipationClub, Long> {

    long countByClub_IdAndStatut(Long clubId, StatutParticipation statut);

    List<ParticipationClub> findByStatut(StatutParticipation statut);

    List<ParticipationClub> findByClubIdAndStatut(Long clubId, StatutParticipation statut);

    List<ParticipationClub> findByClubIdOrderByDateDemandeDesc(Long clubId);

    Optional<ParticipationClub> findByIdEtudiantAndClubId(Long idEtudiant, Long clubId);

    boolean existsByIdEtudiantAndClubIdAndStatut(Long idEtudiant, Long clubId, StatutParticipation statut);

    List<ParticipationClub> findByStatutOrderByScoreDesc(StatutParticipation statut);

    List<ParticipationClub> findByIdEtudiantAndStatut(Long idEtudiant, StatutParticipation statut);
}
