package org.example.club.repository;

import org.example.club.entity.Club;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    
    // Recherche par nom
    Optional<Club> findByNom(String nom);
    
    // Recherche par type
    List<Club> findByType(String type);
    
    // Recherche par statut
    List<Club> findByStatut(String statut);
    
    // Recherche par responsable
    List<Club> findByIdResponsable(Long idResponsable);
    
    // Vérifier l'existence par nom
    boolean existsByNom(String nom);
}

