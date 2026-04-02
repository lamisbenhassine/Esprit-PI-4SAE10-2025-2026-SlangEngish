package org.example.club.service;


import org.example.club.entity.ReunionClub;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReunionClubService {
    
    // Créer une nouvelle réunion
    ReunionClub createReunion(ReunionClub reunion);
    
    // Récupérer toutes les réunions
    List<ReunionClub> getAllReunions();
    
    // Récupérer une réunion par son ID
    Optional<ReunionClub> getReunionById(Long id);
    
    // Mettre à jour une réunion
    ReunionClub updateReunion(Long id, ReunionClub reunion);
    
    // Supprimer une réunion
    void deleteReunion(Long id);
    
    // Récupérer les réunions d'un club
    List<ReunionClub> getReunionsByClub(Long clubId);
    
    // Récupérer les réunions par date
    List<ReunionClub> getReunionsByDate(LocalDate date);
    
    // Récupérer les réunions d'un club par date
    List<ReunionClub> getReunionsByClubAndDate(Long clubId, LocalDate date);
    
    // Récupérer les réunions d'un club, triées par date décroissante
    List<ReunionClub> getReunionsByClubOrderByDateDesc(Long clubId);
    
    // Récupérer les réunions à venir
    List<ReunionClub> getReunionsAVenir(LocalDate date);
}

