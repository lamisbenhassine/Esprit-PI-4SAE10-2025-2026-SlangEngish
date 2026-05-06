package org.example.club.repository;

import org.example.club.entity.ReunionClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReunionClubRepository extends JpaRepository<ReunionClub, Long> {

    long countByClub_Id(Long clubId);

    // Recherche par club
    List<ReunionClub> findByClubId(Long clubId);
    
    // Recherche par date
    List<ReunionClub> findByDate(LocalDate date);
    
    // Recherche par club et date
    List<ReunionClub> findByClubIdAndDate(Long clubId, LocalDate date);
    
    // Recherche par club, triée par date décroissante
    List<ReunionClub> findByClubIdOrderByDateDesc(Long clubId);
    
    // Recherche par date après
    List<ReunionClub> findByDateAfter(LocalDate date);
    
    // Recherche par date avant
    List<ReunionClub> findByDateBefore(LocalDate date);
}

