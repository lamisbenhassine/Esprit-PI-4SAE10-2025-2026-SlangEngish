package org.example.club.service;

import org.example.club.client.UserApiClient;
import org.example.club.client.UserInfoDto;
import org.example.club.dto.ClubMessageDto;
import org.example.club.entity.Club;
import org.example.club.entity.ClubMessage;
import org.example.club.entity.MessageScope;
import org.example.club.entity.ParticipationClub;
import org.example.club.entity.StatutParticipation;
import org.example.club.repository.ClubMessageRepository;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.ParticipationClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClubMessageServiceImpl implements ClubMessageService {

    private final ClubMessageRepository messageRepository;
    private final ClubRepository clubRepository;
    private final ParticipationClubRepository participationRepository;
    private final UserApiClient userApiClient;

    @Autowired
    public ClubMessageServiceImpl(ClubMessageRepository messageRepository,
                                  ClubRepository clubRepository,
                                  ParticipationClubRepository participationRepository,
                                  UserApiClient userApiClient) {
        this.messageRepository = messageRepository;
        this.clubRepository = clubRepository;
        this.participationRepository = participationRepository;
        this.userApiClient = userApiClient;
    }

    @Override
    public List<ClubMessageDto> getMessagesByClub(Long clubId, Long idEtudiant, String scope, String departement) {
        if (!participationRepository.existsByIdEtudiantAndClubIdAndStatut(idEtudiant, clubId, StatutParticipation.ACCEPTED)) {
            throw new IllegalArgumentException("Vous devez être membre du club pour accéder aux messages");
        }
        MessageScope sc = parseScope(scope);
        List<ClubMessage> messages;
        if (sc == MessageScope.DEPARTEMENT) {
            String dep = resolveDepartement(idEtudiant, clubId, departement);
            messages = messageRepository.findByClubIdAndScopeAndDepartementOrderByDateCreationAsc(clubId, sc, dep);
        } else {
            messages = messageRepository.findByClubIdAndScopeOrderByDateCreationAsc(clubId, sc);
        }
        return messages
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ClubMessageDto envoyerMessage(Long clubId, Long idEtudiant, String contenu, String scope, String departement) {
        Club club = clubRepository.findById(clubId)
                .orElseThrow(() -> new IllegalArgumentException("Club non trouvé"));
        if (!participationRepository.existsByIdEtudiantAndClubIdAndStatut(idEtudiant, clubId, StatutParticipation.ACCEPTED)) {
            throw new IllegalArgumentException("Vous devez être membre du club pour envoyer des messages");
        }
        if (contenu == null || contenu.trim().isEmpty()) {
            throw new IllegalArgumentException("Le message ne peut pas être vide");
        }
        ClubMessage msg = new ClubMessage();
        msg.setClub(club);
        msg.setIdEtudiant(idEtudiant);
        msg.setContenu(contenu.trim());
        MessageScope sc = parseScope(scope);
        msg.setScope(sc);
        if (sc == MessageScope.DEPARTEMENT) {
            msg.setDepartement(resolveDepartement(idEtudiant, clubId, departement));
        } else {
            msg.setDepartement(null);
        }
        ClubMessage saved = messageRepository.save(msg);
        return toDto(saved);
    }

    private ClubMessageDto toDto(ClubMessage m) {
        UserInfoDto user = userApiClient.getUserById(m.getIdEtudiant());
        return new ClubMessageDto(
                m.getId(),
                m.getClub().getId(),
                m.getIdEtudiant(),
                m.getContenu(),
                m.getScope() != null ? m.getScope().name() : MessageScope.CLUB.name(),
                m.getDepartement(),
                m.getDateCreation(),
                user != null ? user.getFirstName() : "?",
                user != null ? user.getLastName() : "?"
        );
    }

    private MessageScope parseScope(String scope) {
        if (scope == null || scope.isBlank()) {
            return MessageScope.CLUB;
        }
        try {
            return MessageScope.valueOf(scope.trim().toUpperCase());
        } catch (Exception e) {
            return MessageScope.CLUB;
        }
    }

    private String resolveDepartement(Long idEtudiant, Long clubId, String departement) {
        if (departement != null && !departement.isBlank()) {
            return departement.trim();
        }
        ParticipationClub p = participationRepository.findByIdEtudiantAndClubId(idEtudiant, clubId)
                .orElseThrow(() -> new IllegalArgumentException("Affectation introuvable"));
        if (p.getDepartementAssigne() == null || p.getDepartementAssigne().isBlank()) {
            throw new IllegalArgumentException("Aucun département assigné pour cet étudiant");
        }
        return p.getDepartementAssigne().trim();
    }
}
