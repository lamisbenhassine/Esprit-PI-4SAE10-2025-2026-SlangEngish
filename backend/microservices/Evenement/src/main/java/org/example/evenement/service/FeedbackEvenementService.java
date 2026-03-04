package org.example.evenement.service;

import org.example.evenement.dto.FeedbackDto;
import org.example.evenement.dto.MoyenneFeedbackDto;
import org.example.evenement.entity.FeedbackEvenement;

import java.util.List;

public interface FeedbackEvenementService {

    FeedbackDto createOrUpdateFeedback(Long idEtudiant, Long evenementId, Integer note, String commentaire);

    List<FeedbackDto> getFeedbacksByEvenement(Long evenementId);

    MoyenneFeedbackDto getMoyenneByEvenement(Long evenementId);

    List<FeedbackDto> getAllFeedbacksForAdmin();

    boolean hasEtudiantFeedback(Long idEtudiant, Long evenementId);
}
