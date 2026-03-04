package org.example.evenement.repository;

import org.example.evenement.entity.ListeAttenteEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListeAttenteEvenementRepository extends JpaRepository<ListeAttenteEvenement, Long> {

    List<ListeAttenteEvenement> findByIdEtudiant(Long idEtudiant);

    List<ListeAttenteEvenement> findByEvenementIdOrderByPositionAsc(Long evenementId);

    Optional<ListeAttenteEvenement> findFirstByEvenementIdOrderByPositionAsc(Long evenementId);

    boolean existsByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);

    Optional<ListeAttenteEvenement> findByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);

    @Modifying
    @Query("DELETE FROM ListeAttenteEvenement lae WHERE lae.evenement.id = :evenementId AND lae.idEtudiant = :idEtudiant")
    void deleteByEvenementIdAndIdEtudiant(@Param("evenementId") Long evenementId, @Param("idEtudiant") Long idEtudiant);

    long countByEvenementId(Long evenementId);
}
