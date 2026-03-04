package org.example.evenement.repository;

import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface EvenementRepository extends JpaRepository<Evenement, Long> {
    
    // Recherche par titre
    Optional<Evenement> findByTitre(String titre);
    
    // Recherche par type
    List<Evenement> findByType(String type);
    
    // Recherche par statut
    List<Evenement> findByStatus(EventStatus status);
    
    // Recherche par date
    List<Evenement> findByDate(LocalDate date);
    
    // Recherche par date après
    List<Evenement> findByDateAfter(LocalDate date);
    
    // Recherche par date avant
    List<Evenement> findByDateBefore(LocalDate date);
    
    // Recherche par lieu
    List<Evenement> findByLieu(String lieu);
    
    // Vérifier l'existence par titre
    boolean existsByTitre(String titre);
}

