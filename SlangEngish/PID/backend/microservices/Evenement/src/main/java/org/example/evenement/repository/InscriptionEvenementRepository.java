package org.example.evenement.repository;

import org.example.evenement.entity.InscriptionEvenement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InscriptionEvenementRepository extends JpaRepository<InscriptionEvenement, Long> {
    
    // Recherche par étudiant
    List<InscriptionEvenement> findByIdEtudiant(Long idEtudiant);
    
    // Recherche par événement
    List<InscriptionEvenement> findByEvenementId(Long evenementId);
    
    // Vérifier si un étudiant est déjà inscrit à un événement
    boolean existsByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);
    
    // Compter les inscriptions pour un événement
    long countByEvenementId(Long evenementId);
    
    // Recherche par étudiant et événement
    Optional<InscriptionEvenement> findByIdEtudiantAndEvenementId(Long idEtudiant, Long evenementId);
}

