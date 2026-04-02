package org.example.club.repository;

import org.example.club.entity.PostClub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostClubRepository extends JpaRepository<PostClub, Long> {
    
    // Recherche par club
    List<PostClub> findByClubId(Long clubId);
    
    // Recherche par auteur
    List<PostClub> findByIdAuteur(Long idAuteur);
    
    // Recherche par club et auteur
    List<PostClub> findByClubIdAndIdAuteur(Long clubId, Long idAuteur);
    
    // Recherche par club, triée par date décroissante
    List<PostClub> findByClubIdOrderByDatePostDesc(Long clubId);
}

