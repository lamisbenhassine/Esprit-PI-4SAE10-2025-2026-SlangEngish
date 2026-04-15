package org.example.club.service;

import org.example.club.dto.DemandeParticipationDto;
import org.example.club.dto.DemandeParticipationViewDto;
import org.example.club.dto.MembreClubDto;
import org.example.club.dto.ResultDemandeDto;
import org.example.club.entity.Club;
import org.example.club.entity.StatutParticipation;

import java.util.List;

public interface ParticipationClubService {

    /**
     * Demande de rejoindre un club avec formulaire. Calcule le score et enregistre.
     */
    ResultDemandeDto demanderRejoindre(DemandeParticipationDto dto);

    /**
     * Liste des demandes en attente (pour backoffice)
     */
    List<DemandeParticipationViewDto> getDemandesEnAttente(Long clubId);

    void accepter(Long idDemande, String departementAssigne);

    void refuser(Long idDemande);

    /**
     * Statut de participation d'un étudiant pour un club (PENDING, ACCEPTED, REFUSED ou null)
     */
    StatutParticipation getStatutParticipation(Long idEtudiant, Long idClub);

    /**
     * Liste des clubs dont l'étudiant est membre (ACCEPTED)
     */
    List<Club> getClubsMembre(Long idEtudiant);

    /**
     * Département affecté pour un étudiant membre d'un club
     */
    String getDepartementAffecte(Long idEtudiant, Long idClub);

    List<MembreClubDto> getMembresClub(Long idClub);
    void bloquerMembre(Long participationId);
    void supprimerMembre(Long participationId);

    /**
     * Calcule le score à partir du texte de motivation (0-100)
     * Analyse : longueur, mots-clés positifs, structure
     */
    double calculerScoreTexteMotivation(String texteMotivation);

    /**
     * Calcule le score à partir des réponses numériques (0-100) - rétrocompatibilité
     */
    double calculerScore(java.util.Map<String, Integer> reponses);
}
