package org.example.club.service;

import org.example.club.entity.Club;
import org.example.club.entity.ReunionClub;
import org.example.club.entity.ReunionAudience;
import org.example.club.entity.TypeReunion;
import org.example.club.entity.ParticipationClub;
import org.example.club.entity.StatutParticipation;
import org.example.club.client.UserApiClient;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.ParticipationClubRepository;
import org.example.club.repository.ReunionClubRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReunionClubServiceImpl implements ReunionClubService {

    @Autowired
    private ReunionClubRepository reunionRepository;
    
    @Autowired
    private ClubRepository clubRepository;

    @Autowired
    private ParticipationClubRepository participationRepository;

    @Autowired
    private UserApiClient userApiClient;

    @Autowired
    private EmailService emailService;

    @Value("${app.reunion.online-base-url:https://meet.jit.si}")
    private String onlineMeetingBaseUrl;

    @Override
    public ReunionClub createReunion(ReunionClub reunion) {
        if (reunion.getDate() == null) {
            throw new IllegalArgumentException("La date de la réunion est obligatoire");
        }
        if (reunion.getClub() == null || reunion.getClub().getId() == null) {
            throw new IllegalArgumentException("Le club est obligatoire");
        }
        if (reunion.getTypeReunion() == null) {
            reunion.setTypeReunion(TypeReunion.PRESENTIEL);
        }
        if (reunion.getAudience() == null) {
            reunion.setAudience(ReunionAudience.TOUS_CLUB);
        }

        if (reunion.getTypeReunion() == TypeReunion.PRESENTIEL) {
            if (reunion.getLieu() == null || reunion.getLieu().isBlank()) {
                throw new IllegalArgumentException("Le lieu est obligatoire pour une réunion présentielle");
            }
            reunion.setLienReunion(null);
        } else {
            if (reunion.getLienReunion() == null || reunion.getLienReunion().isBlank()) {
                reunion.setLienReunion(generateOnlineMeetingLink());
            }
            reunion.setLieu(null);
        }
        
        // Vérifier si le club existe
        Club club = clubRepository.findById(reunion.getClub().getId())
                .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID: " + reunion.getClub().getId()));
        
        reunion.setClub(club);
        ReunionClub saved = reunionRepository.save(reunion);

        Set<String> depsCibles = parseCsvSet(saved.getDepartementsCibles());
        boolean tous = saved.getAudience() == ReunionAudience.TOUS_CLUB;

        // Notifier les membres ciblés par email.
        List<ParticipationClub> membres = participationRepository.findByClubIdAndStatut(club.getId(), StatutParticipation.ACCEPTED);
        for (ParticipationClub p : membres) {
            if (!tous) {
                String dep = p.getDepartementAssigne() == null ? "" : p.getDepartementAssigne().trim();
                if (!depsCibles.contains(dep)) {
                    continue;
                }
            }
            var user = userApiClient.getUserById(p.getIdEtudiant());
            emailService.sendReunionInvitationEmail(user, club, saved);
        }

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getAllReunions() {
        return reunionRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ReunionClub> getReunionById(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de la réunion ne peut pas être null");
        }
        return reunionRepository.findById(id);
    }

    @Override
    public ReunionClub updateReunion(Long id, ReunionClub reunion) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de la réunion ne peut pas être null");
        }
        
        ReunionClub existingReunion = reunionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Réunion non trouvée avec l'ID: " + id));
        
        if (reunion.getDate() != null) {
            existingReunion.setDate(reunion.getDate());
        }
        
        if (reunion.getHeure() != null) {
            existingReunion.setHeure(reunion.getHeure());
        }
        
        if (reunion.getLieu() != null) {
            existingReunion.setLieu(reunion.getLieu());
        }
        if (reunion.getTypeReunion() != null) {
            existingReunion.setTypeReunion(reunion.getTypeReunion());
        }
        if (reunion.getLienReunion() != null) {
            existingReunion.setLienReunion(reunion.getLienReunion());
        }
        if (reunion.getAudience() != null) {
            existingReunion.setAudience(reunion.getAudience());
        }
        if (reunion.getDepartementsCibles() != null) {
            existingReunion.setDepartementsCibles(reunion.getDepartementsCibles());
        }
        
        if (reunion.getClub() != null && reunion.getClub().getId() != null) {
            Club club = clubRepository.findById(reunion.getClub().getId())
                    .orElseThrow(() -> new RuntimeException("Club non trouvé avec l'ID: " + reunion.getClub().getId()));
            existingReunion.setClub(club);
        }
        
        return reunionRepository.save(existingReunion);
    }

    private String generateOnlineMeetingLink() {
        String token = "club-" + UUID.randomUUID().toString().replace("-", "");
        String base = onlineMeetingBaseUrl == null || onlineMeetingBaseUrl.isBlank()
                ? "https://meet.jit.si"
                : onlineMeetingBaseUrl;
        if (base.endsWith("/")) {
            return base + token;
        }
        return base + "/" + token;
    }

    private Set<String> parseCsvSet(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    @Override
    public void deleteReunion(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("L'ID de la réunion ne peut pas être null");
        }
        
        if (!reunionRepository.existsById(id)) {
            throw new RuntimeException("Réunion non trouvée avec l'ID: " + id);
        }
        
        reunionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getReunionsByClub(Long clubId) {
        return reunionRepository.findByClubId(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getReunionsByDate(LocalDate date) {
        return reunionRepository.findByDate(date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getReunionsByClubAndDate(Long clubId, LocalDate date) {
        return reunionRepository.findByClubIdAndDate(clubId, date);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getReunionsByClubOrderByDateDesc(Long clubId) {
        return reunionRepository.findByClubIdOrderByDateDesc(clubId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReunionClub> getReunionsAVenir(LocalDate date) {
        return reunionRepository.findByDateAfter(date);
    }
}

