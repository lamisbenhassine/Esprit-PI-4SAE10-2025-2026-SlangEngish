package org.example.club.repository;

import org.example.club.entity.PostClub;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostClubRepository extends JpaRepository<PostClub, Long> {

    long countByClub_Id(Long clubId);

    @EntityGraph(attributePaths = {"club"})
    @Query("select p from PostClub p order by p.datePost desc, p.id desc")
    Page<PostClub> findAllPagedRecent(Pageable pageable);

    @EntityGraph(attributePaths = {"club"})
    @Query("select p from PostClub p where p.club.id = :clubId order by p.datePost desc, p.id desc")
    Page<PostClub> findPagedByClub(@Param("clubId") Long clubId, Pageable pageable);
    
    // Recherche par club
    List<PostClub> findByClubId(Long clubId);
    
    // Recherche par auteur
    List<PostClub> findByIdAuteur(Long idAuteur);
    
    // Recherche par club et auteur
    List<PostClub> findByClubIdAndIdAuteur(Long clubId, Long idAuteur);
    
    // Recherche par club, triée par date décroissante
    List<PostClub> findByClubIdOrderByDatePostDesc(Long clubId);
}

