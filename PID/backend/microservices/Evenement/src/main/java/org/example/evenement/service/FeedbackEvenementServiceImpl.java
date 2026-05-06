package org.example.evenement.service;

import org.example.evenement.client.UserApiClient;
import org.example.evenement.client.UserInfoDto;
import org.example.evenement.dto.FeedbackDto;
import org.example.evenement.dto.MoyenneFeedbackDto;
import org.example.evenement.entity.Evenement;
import org.example.evenement.entity.FeedbackEvenement;
import org.example.evenement.repository.EvenementRepository;
import org.example.evenement.repository.FeedbackEvenementRepository;
import org.example.evenement.repository.InscriptionEvenementRepository;
import org.example.evenement.sentiment.FeedbackSentiment;
import org.example.evenement.sentiment.FeedbackSentimentAnalyzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class FeedbackEvenementServiceImpl implements FeedbackEvenementService {

    @Autowired
    private FeedbackEvenementRepository feedbackRepository;

    @Autowired
    private EvenementRepository evenementRepository;

    @Autowired
    private InscriptionEvenementRepository inscriptionRepository;

    @Autowired
    private UserApiClient userApiClient;

    @Autowired
    private FeedbackSentimentAnalyzer sentimentAnalyzer;

    @Autowired
    private EmailService emailService;

    @Override
    public FeedbackDto createOrUpdateFeedback(Long idEtudiant, Long evenementId, Integer note, String commentaire) {
        if (note == null || note < 1 || note > 5) {
            throw new IllegalArgumentException("La note doit être entre 1 et 5");
        }
        Evenement evenement = evenementRepository.findById(evenementId)
                .orElseThrow(() -> new IllegalArgumentException("Événement non trouvé"));

        // Vérifier que l'étudiant a participé (inscrit) à l'événement
        if (!inscriptionRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId)) {
            throw new IllegalArgumentException("Vous devez avoir participé à cet événement pour donner un avis");
        }

        FeedbackEvenement feedback = feedbackRepository.findByIdEtudiantAndEvenementId(idEtudiant, evenementId)
                .orElse(new FeedbackEvenement());

        feedback.setIdEtudiant(idEtudiant);
        feedback.setEvenement(evenement);
        feedback.setNote(note);
        feedback.setCommentaire(commentaire != null ? commentaire.trim() : null);
        feedback.setDateCreation(feedback.getDateCreation() != null ? feedback.getDateCreation() : LocalDateTime.now());

        FeedbackEvenement saved = feedbackRepository.save(feedback);
        FeedbackSentiment sentiment = sentimentAnalyzer.analyze(saved.getCommentaire(), saved.getNote());
        if (sentimentAnalyzer.shouldNotifyAdmin(sentiment, saved.getCommentaire(), saved.getNote())) {
            emailService.sendNegativeFeedbackAlertEvenement(
                    evenement.getTitre(),
                    evenementId,
                    idEtudiant,
                    getNomEtudiant(idEtudiant),
                    saved.getNote(),
                    saved.getCommentaire(),
                    sentiment
            );
        }
        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackDto> getFeedbacksByEvenement(Long evenementId) {
        return feedbackRepository.findByEvenementId(evenementId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MoyenneFeedbackDto getMoyenneByEvenement(Long evenementId) {
        Double moyenne = feedbackRepository.getMoyenneByEvenementId(evenementId);
        long count = feedbackRepository.countByEvenementId(evenementId);
        return new MoyenneFeedbackDto(evenementId, moyenne != null ? Math.round(moyenne * 10.0) / 10.0 : null, count);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FeedbackDto> getAllFeedbacksForAdmin() {
        return feedbackRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasEtudiantFeedback(Long idEtudiant, Long evenementId) {
        return feedbackRepository.existsByIdEtudiantAndEvenementId(idEtudiant, evenementId);
    }

    private FeedbackDto toDto(FeedbackEvenement f) {
        String nomEtudiant = getNomEtudiant(f.getIdEtudiant());
        FeedbackSentiment s = sentimentAnalyzer.analyze(f.getCommentaire(), f.getNote());
        return new FeedbackDto(
                f.getId(),
                f.getIdEtudiant(),
                f.getEvenement() != null ? f.getEvenement().getId() : null,
                f.getNote(),
                f.getCommentaire(),
                f.getDateCreation(),
                nomEtudiant,
                s.name()
        );
    }

    private String getNomEtudiant(Long idEtudiant) {
        UserInfoDto user = userApiClient.getUserById(idEtudiant);
        if (user == null) return "Invité";
        String first = user.getFirstName() != null ? user.getFirstName() : "";
        String last = user.getLastName() != null ? user.getLastName() : "";
        return (first + " " + last).trim().isEmpty() ? "Invité" : (first + " " + last).trim();
    }
}
