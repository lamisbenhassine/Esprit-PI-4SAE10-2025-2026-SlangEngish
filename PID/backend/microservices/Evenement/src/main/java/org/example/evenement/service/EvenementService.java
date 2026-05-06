package org.example.evenement.service;

import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.EventStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface EvenementService {
    
    // Créer un nouvel événement
    Evenement createEvenement(Evenement evenement);
    
    // Récupérer tous les événements
    List<Evenement> getAllEvenements();
    
    // Récupérer un événement par son ID
    Optional<Evenement> getEvenementById(Long id);
    
    // Mettre à jour un événement
    Evenement updateEvenement(Long id, Evenement evenement);
    
    // Supprimer un événement
    void deleteEvenement(Long id);
    
    // Recherche par titre
    Optional<Evenement> getEvenementByTitre(String titre);
    
    // Recherche par type
    List<Evenement> getEvenementsByType(String type);
    
    // Recherche par statut
    List<Evenement> getEvenementsByStatus(EventStatus status);
    
    // Recherche par date
    List<Evenement> getEvenementsByDate(LocalDate date);
    
    // Recherche par lieu
    List<Evenement> getEvenementsByLieu(String lieu);
    
    // Recherche des événements à venir
    List<Evenement> getEvenementsAVenir(LocalDate date);
}

