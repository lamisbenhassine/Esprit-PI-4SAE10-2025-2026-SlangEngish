package org.example.club.service;

import org.example.club.client.UserApiClient;
import org.example.club.client.UserInfoDto;
import org.example.club.dto.ClubFeedbackSummaryDto;
import org.example.club.dto.FeedbackClubDto;
import org.example.club.dto.FeedbackClubRequestDto;
import org.example.club.dto.FeedbackClubStatsDto;
import org.example.club.entity.Club;
import org.example.club.entity.FeedbackClub;
import org.example.club.entity.StatutParticipation;
import org.example.club.repository.ClubRepository;
import org.example.club.repository.FeedbackClubRepository;
import org.example.club.repository.ParticipationClubRepository;
import org.example.club.sentiment.FeedbackSentiment;
import org.example.club.sentiment.FeedbackSentimentAnalyzer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FeedbackClubServiceImpl implements FeedbackClubService {

    private final FeedbackClubRepository feedbackRepository;
    private final ClubRepository clubRepository;
    private final ParticipationClubRepository participationRepository;
    private final UserApiClient userApiClient;
    private final FeedbackSentimentAnalyzer sentimentAnalyzer;
    private final EmailService emailService;

    public FeedbackClubServiceImpl(FeedbackClubRepository feedbackRepository,
                                   ClubRepository clubRepository,
                                   ParticipationClubRepository participationRepository,
                                   UserApiClient userApiClient,
                                   FeedbackSentimentAnalyzer sentimentAnalyzer,
                                   EmailService emailService) {
        this.feedbackRepository = feedbackRepository;
        this.clubRepository = clubRepository;
        this.participationRepository = participationRepository;
        this.userApiClient = userApiClient;
        this.sentimentAnalyzer = sentimentAnalyzer;
        this.emailService = emailService;
    }

    @Override
    @Transactional
    public FeedbackClubDto createOrUpdate(FeedbackClubRequestDto request) {
        if (request == null || request.getIdEtudiant() == null || request.getIdClub() == null) {
            throw new IllegalArgumentException("idEtudiant et idClub sont requis");
        }
        if (request.getNote() == null || request.getNote() < 1 || request.getNote() > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }
        if (!participationRepository.existsByIdEtudiantAndClubIdAndStatut(
                request.getIdEtudiant(), request.getIdClub(), StatutParticipation.ACCEPTED)) {
            throw new IllegalArgumentException("Seuls les membres acceptés peuvent envoyer un feedback");
        }

        Club club = clubRepository.findById(request.getIdClub())
                .orElseThrow(() -> new IllegalArgumentException("Club non trouvé"));

        FeedbackClub feedback = feedbackRepository.findByIdEtudiantAndClubId(request.getIdEtudiant(), request.getIdClub())
                .orElseGet(FeedbackClub::new);

        feedback.setIdEtudiant(request.getIdEtudiant());
        feedback.setClub(club);
        feedback.setNote(request.getNote());
        feedback.setCommentaire(request.getCommentaire() == null ? null : request.getCommentaire().trim());

        FeedbackClub saved = feedbackRepository.save(feedback);
        FeedbackSentiment sentiment = sentimentAnalyzer.analyze(saved.getCommentaire(), saved.getNote());
        if (sentimentAnalyzer.shouldNotifyAdmin(sentiment, saved.getCommentaire(), saved.getNote())) {
            UserInfoDto u = userApiClient.getUserById(request.getIdEtudiant());
            String full = u != null
                    ? ((u.getFirstName() != null ? u.getFirstName() : "") + " " + (u.getLastName() != null ? u.getLastName() : "")).trim()
                    : "id=" + request.getIdEtudiant();
            emailService.sendNegativeFeedbackAlertClub(
                    club.getNom(),
                    club.getId(),
                    request.getIdEtudiant(),
                    full.isEmpty() ? "id=" + request.getIdEtudiant() : full,
                    saved.getNote(),
                    saved.getCommentaire(),
                    sentiment
            );
        }
        return toDto(saved);
    }

    @Override
    public List<FeedbackClubDto> getByClub(Long clubId) {
        return feedbackRepository.findByClubIdOrderByDateCreationDesc(clubId)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<FeedbackClubDto> getAll() {
        return feedbackRepository.findAll().stream()
                .sorted(Comparator.comparing(FeedbackClub::getDateCreation).reversed())
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public FeedbackClubStatsDto getAdminStats() {
        List<FeedbackClub> all = feedbackRepository.findAll();
        long total = all.size();
        double moyenne = total == 0 ? 0.0 : round1(all.stream().mapToInt(FeedbackClub::getNote).average().orElse(0));

        Map<Integer, Long> dist = all.stream().collect(Collectors.groupingBy(FeedbackClub::getNote, Collectors.counting()));
        List<FeedbackClubStatsDto.NoteCountDto> distribution = new ArrayList<>();
        for (int n = 1; n <= 5; n++) {
            distribution.add(new FeedbackClubStatsDto.NoteCountDto(n, dist.getOrDefault(n, 0L)));
        }

        Map<Long, List<FeedbackClub>> byClub = all.stream().collect(Collectors.groupingBy(f -> f.getClub().getId()));
        List<FeedbackClubStatsDto.ClubMoyenneDto> topClubs = byClub.entrySet().stream()
                .map(e -> {
                    List<FeedbackClub> list = e.getValue();
                    Club c = list.get(0).getClub();
                    double m = round1(list.stream().mapToInt(FeedbackClub::getNote).average().orElse(0));
                    return new FeedbackClubStatsDto.ClubMoyenneDto(c.getId(), c.getNom(), m, list.size());
                })
                .sorted(Comparator.comparing(FeedbackClubStatsDto.ClubMoyenneDto::getMoyenne).reversed())
                .limit(8)
                .collect(Collectors.toList());

        DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM");
        Map<String, List<FeedbackClub>> byMonth = all.stream()
                .collect(Collectors.groupingBy(f -> f.getDateCreation().format(monthFmt)));
        List<FeedbackClubStatsDto.TendanceMensuelleDto> tendance = byMonth.entrySet().stream()
                .map(e -> {
                    List<FeedbackClub> list = e.getValue();
                    double m = round1(list.stream().mapToInt(FeedbackClub::getNote).average().orElse(0));
                    return new FeedbackClubStatsDto.TendanceMensuelleDto(e.getKey(), list.size(), m);
                })
                .sorted(Comparator.comparing(FeedbackClubStatsDto.TendanceMensuelleDto::getMois))
                .collect(Collectors.toList());

        return new FeedbackClubStatsDto(total, moyenne, distribution, topClubs, tendance);
    }

    @Override
    public List<ClubFeedbackSummaryDto> getPublicSummaries() {
        return feedbackRepository.summarizeByClub().stream()
                .map(row -> {
                    Long clubId = (Long) row[0];
                    double avg = row[1] == null ? 0.0 : ((Number) row[1]).doubleValue();
                    long count = ((Number) row[2]).longValue();
                    return new ClubFeedbackSummaryDto(clubId, round1(avg), count);
                })
                .collect(Collectors.toList());
    }

    private FeedbackClubDto toDto(FeedbackClub f) {
        UserInfoDto u = userApiClient.getUserById(f.getIdEtudiant());
        FeedbackSentiment s = sentimentAnalyzer.analyze(f.getCommentaire(), f.getNote());
        return new FeedbackClubDto(
                f.getId(),
                f.getIdEtudiant(),
                f.getClub().getId(),
                f.getClub().getNom(),
                f.getNote(),
                f.getCommentaire(),
                f.getDateCreation(),
                u != null ? u.getFirstName() : "?",
                u != null ? u.getLastName() : "?",
                s.name()
        );
    }

    private double round1(double d) {
        return Math.round(d * 10.0) / 10.0;
    }
}

