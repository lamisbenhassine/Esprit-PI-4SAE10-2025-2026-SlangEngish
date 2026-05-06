package org.example.evenement.service;

import org.example.evenement.dto.InscriptionResultDto;
import org.example.evenement.dto.StatutInscriptionDto;
import org.example.evenement.entity.InscriptionEvenement;

import java.util.List;
import java.util.Optional;

public interface InscriptionEvenementService {
    
    // Créer une nouvelle inscription (ou ajouter en liste d'attente si complet)
    InscriptionResultDto createInscription(InscriptionEvenement inscription);
    
    // Récupérer toutes les inscriptions
    List<InscriptionEvenement> getAllInscriptions();
    
    // Récupérer une inscription par son ID
    Optional<InscriptionEvenement> getInscriptionById(Long id);
    
    // Mettre à jour une inscription
    InscriptionEvenement updateInscription(Long id, InscriptionEvenement inscription);
    
    // Supprimer une inscription
    void deleteInscription(Long id);
    
    // Récupérer les inscriptions d'un étudiant
    List<InscriptionEvenement> getInscriptionsByEtudiant(Long idEtudiant);
    
    // Récupérer les inscriptions d'un événement
    List<InscriptionEvenement> getInscriptionsByEvenement(Long evenementId);
    
    // Vérifier si un étudiant est inscrit à un événement
    boolean isEtudiantInscrit(Long idEtudiant, Long evenementId);

    // Vérifier si un étudiant est en liste d'attente pour un événement
    boolean isEtudiantEnListeAttente(Long idEtudiant, Long evenementId);

    // Obtenir la position en liste d'attente (0 si non en liste)
    int getPositionListeAttente(Long idEtudiant, Long evenementId);

    // Obtenir le statut complet (inscrit, liste attente, position)
    StatutInscriptionDto getStatutInscription(Long idEtudiant, Long evenementId);
    
    // Compter les inscriptions pour un événement
    long countInscriptionsByEvenement(Long evenementId);
    
    // Désinscrire un étudiant d'un événement
    void desinscrireEtudiant(Long idEtudiant, Long evenementId);
}

