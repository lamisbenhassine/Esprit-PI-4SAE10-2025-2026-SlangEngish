package org.example.club.service;


import org.example.club.entity.Club;

import java.util.List;
import java.util.Optional;

public interface ClubService {
    
    // Créer un nouveau club
    Club createClub(Club club);
    
    // Récupérer tous les clubs
    List<Club> getAllClubs();
    
    // Récupérer un club par son ID
    Optional<Club> getClubById(Long id);
    
    // Mettre à jour un club
    Club updateClub(Long id, Club club);
    
    // Supprimer un club
    void deleteClub(Long id);
    
    // Recherche par nom
    Optional<Club> getClubByNom(String nom);
    
    // Recherche par type
    List<Club> getClubsByType(String type);
    
    // Recherche par statut
    List<Club> getClubsByStatut(String statut);
    
    // Recherche par responsable
    List<Club> getClubsByResponsable(Long idResponsable);
}

