package org.example.club.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.club.client.UserApiClient;
import org.example.club.client.UserInfoDto;
import org.example.club.dto.DemandeParticipationDto;
import org.example.club.dto.DemandeParticipationViewDto;
import org.example.club.dto.MembreClubDto;
import org.example.club.dto.ResultDemandeDto;
import org.example.club.entity.Club;
import org.example.club.entity.ParticipationClub;
import org.example.club.entity.StatutParticipation;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.ParticipationClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ParticipationClubServiceImpl implements ParticipationClubService {

    private final ParticipationClubRepository participationRepository;
    private final ClubRepository clubRepository;
    private final UserApiClient userApiClient;
    private final EmailService emailService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    public ParticipationClubServiceImpl(ParticipationClubRepository participationRepository,
                                        ClubRepository clubRepository,
                                        UserApiClient userApiClient,
                                        EmailService emailService) {
        this.participationRepository = participationRepository;
        this.clubRepository = clubRepository;
        this.userApiClient = userApiClient;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public ResultDemandeDto demanderRejoindre(DemandeParticipationDto dto) {
        Club club = clubRepository.findById(dto.getIdClub())
                .orElseThrow(() -> new IllegalArgumentException("Club non trouvé"));

        if (participationRepository.existsByIdEtudiantAndClubIdAndStatut(
                dto.getIdEtudiant(), dto.getIdClub(), StatutParticipation.PENDING)) {
            throw new IllegalArgumentException("Vous avez déjà une demande en attente pour ce club");
        }

        if (participationRepository.existsByIdEtudiantAndClubIdAndStatut(
                dto.getIdEtudiant(), dto.getIdClub(), StatutParticipation.ACCEPTED)) {
            throw new IllegalArgumentException("Vous êtes déjà membre de ce club");
        }

        String texteMotivation = (dto.getTexteMotivation() != null) ? dto.getTexteMotivation().trim() : "";
        if (texteMotivation.isEmpty()) {
            throw new IllegalArgumentException("Le texte de motivation est requis");
        }

        double scoreNumerique = calculerScore(dto.getReponses());
        double scoreTexte = calculerScoreTexteMotivation(texteMotivation);
        double score = (scoreNumerique + scoreTexte) / 2.0;

        String reponsesJson = null;
        if (dto.getReponses() != null && !dto.getReponses().isEmpty()) {
            try {
                reponsesJson = objectMapper.writeValueAsString(dto.getReponses());
            } catch (JsonProcessingException e) {
                reponsesJson = "{}";
            }
        }

        ParticipationClub participation = new ParticipationClub();
        participation.setIdEtudiant(dto.getIdEtudiant());
        participation.setClub(club);
        participation.setDateDemande(LocalDateTime.now());
        participation.setStatut(StatutParticipation.PENDING);
        participation.setScore(score);
        participation.setTexteMotivation(texteMotivation.trim());
        participation.setReponsesFormulaire(reponsesJson);
        participation.setDepartementSouhaite(dto.getDepartementSouhaite());

        ParticipationClub saved = participationRepository.save(participation);

        return new ResultDemandeDto(
                saved.getId(),
                score,
                "Demande envoyée. Votre score est de " + String.format("%.1f", score) + "/100. L'administrateur traitera votre demande."
        );
    }

    @Override
    public List<DemandeParticipationViewDto> getDemandesEnAttente(Long clubId) {
        List<ParticipationClub> demandes = clubId != null
                ? participationRepository.findByClubIdAndStatut(clubId, StatutParticipation.PENDING)
                : participationRepository.findByStatutOrderByScoreDesc(StatutParticipation.PENDING);

        return demandes.stream()
                .map(this::toViewDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void accepter(Long idDemande, String departementAssigne) {
        ParticipationClub p = participationRepository.findById(idDemande)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));
        if (p.getStatut() != StatutParticipation.PENDING) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée");
        }
        p.setStatut(StatutParticipation.ACCEPTED);
        p.setDepartementAssigne(departementAssigne);
        participationRepository.save(p);

        // Envoi d'un email avec badge membre a l'etudiant.
        UserInfoDto user = userApiClient.getUserById(p.getIdEtudiant());
        emailService.sendAcceptanceBadgeEmail(user, p.getClub());
    }

    @Override
    @Transactional
    public void refuser(Long idDemande) {
        ParticipationClub p = participationRepository.findById(idDemande)
                .orElseThrow(() -> new IllegalArgumentException("Demande non trouvée"));
        if (p.getStatut() != StatutParticipation.PENDING) {
            throw new IllegalArgumentException("Cette demande a déjà été traitée");
        }
        p.setStatut(StatutParticipation.REFUSED);
        participationRepository.save(p);
    }

    @Override
    public StatutParticipation getStatutParticipation(Long idEtudiant, Long idClub) {
        return participationRepository.findByIdEtudiantAndClubId(idEtudiant, idClub)
                .map(ParticipationClub::getStatut)
                .orElse(null);
    }

    @Override
    public List<Club> getClubsMembre(Long idEtudiant) {
        return participationRepository.findByIdEtudiantAndStatut(idEtudiant, StatutParticipation.ACCEPTED)
                .stream()
                .map(ParticipationClub::getClub)
                .map(this::toSimpleClub)
                .collect(Collectors.toList());
    }

    @Override
    public double calculerScoreTexteMotivation(String texteMotivation) {
        if (texteMotivation == null || texteMotivation.trim().isEmpty()) return 0;

        String texte = texteMotivation.trim().toLowerCase();
        int len = texte.length();

        // Partie 1 : Score basé sur la longueur (0-50 pts max)
        // 50-100 chars = 15, 100-200 = 25, 200-300 = 35, 300+ = 50
        double scoreLongueur = 0;
        if (len >= 50) scoreLongueur = 15;
        if (len >= 100) scoreLongueur = 25;
        if (len >= 200) scoreLongueur = 35;
        if (len >= 300) scoreLongueur = 50;

        // Partie 2 : Mots-clés positifs (0-50 pts max)
        String[] motsPositifs = {"motivation", "motivé", "passion", "passionné", "intéressé", "intérêt",
                "aimer", "apprendre", "développer", "partager", "engagement", "sérieux",
                "dynamique", "curieux", "enrichir", "contribution", "collaboration",
                "expérience", "compétences", "objectif", "projet", "déterminé"};
        int nbMotsTrouves = 0;
        for (String mot : motsPositifs) {
            if (texte.contains(mot)) nbMotsTrouves++;
        }
        double scoreMots = Math.min(50, nbMotsTrouves * 5.0);

        return Math.min(100, scoreLongueur + scoreMots);
    }

    @Override
    public double calculerScore(Map<String, Integer> reponses) {
        if (reponses == null || reponses.isEmpty()) return 0;

        int experience = reponses.getOrDefault("experience", 0);
        int motivation = reponses.getOrDefault("motivation", 0);
        int disponibilite = reponses.getOrDefault("disponibilite", 0);

        experience = Math.max(0, Math.min(10, experience));
        motivation = Math.max(0, Math.min(10, motivation));
        disponibilite = Math.max(0, Math.min(20, disponibilite));

        double part1 = experience / 10.0;
        double part2 = motivation / 10.0;
        double part3 = disponibilite / 20.0;
        return ((part1 + part2 + part3) / 3.0) * 100.0;
    }

    private DemandeParticipationViewDto toViewDto(ParticipationClub p) {
        UserInfoDto user = userApiClient.getUserById(p.getIdEtudiant());
        return new DemandeParticipationViewDto(
                p.getId(),
                p.getIdEtudiant(),
                p.getClub().getId(),
                p.getClub().getNom(),
                p.getDateDemande(),
                p.getStatut(),
                p.getScore(),
                p.getReponsesFormulaire(),
                p.getTexteMotivation(),
                p.getDepartementSouhaite(),
                p.getDepartementAssigne(),
                user != null ? user.getFirstName() : "?",
                user != null ? user.getLastName() : "?",
                user != null ? user.getEmail() : "?"
        );
    }

    /**
     * Retourne une projection simple pour éviter les proxys/lazy collections dans la réponse JSON.
     */
    private Club toSimpleClub(Club c) {
        Club club = new Club();
        club.setId(c.getId());
        club.setNom(c.getNom());
        club.setDescription(c.getDescription());
        club.setType(c.getType());
        club.setImage(c.getImage());
        club.setStatut(c.getStatut());
        club.setDateCreation(c.getDateCreation());
        club.setIdResponsable(c.getIdResponsable());
        club.setDepartements(c.getDepartements());
        return club;
    }

    @Override
    public String getDepartementAffecte(Long idEtudiant, Long idClub) {
        return participationRepository.findByIdEtudiantAndClubId(idEtudiant, idClub)
                .map(ParticipationClub::getDepartementAssigne)
                .orElse(null);
    }

    @Override
    public List<MembreClubDto> getMembresClub(Long idClub) {
        return participationRepository.findByClubIdAndStatut(idClub, StatutParticipation.ACCEPTED)
                .stream()
                .map(p -> {
                    UserInfoDto u = userApiClient.getUserById(p.getIdEtudiant());
                    return new MembreClubDto(
                            p.getId(),
                            p.getIdEtudiant(),
                            u != null ? u.getFirstName() : "?",
                            u != null ? u.getLastName() : "?",
                            u != null ? u.getEmail() : "?",
                            p.getDepartementAssigne(),
                            p.getStatut().name()
                    );
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void bloquerMembre(Long participationId) {
        ParticipationClub p = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        if (p.getStatut() != StatutParticipation.ACCEPTED) {
            throw new IllegalArgumentException("Seuls les membres acceptés peuvent être bloqués");
        }
        p.setStatut(StatutParticipation.BLOCKED);
        participationRepository.save(p);
    }

    @Override
    @Transactional
    public void supprimerMembre(Long participationId) {
        ParticipationClub p = participationRepository.findById(participationId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable"));
        participationRepository.delete(p);
    }
}
