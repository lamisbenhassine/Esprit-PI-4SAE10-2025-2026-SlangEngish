package com.school.schoolservice.application.service.impl;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.enums.ApplicationStatus;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.application.service.ApplicationService;
import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.common.service.EmailService;
import com.school.schoolservice.fraud.dto.FraudResultDto;
import com.school.schoolservice.fraud.service.FraudDetectionService;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ApplicationServiceImpl implements ApplicationService {

  private final ApplicationRepository repository;
  private final JobOfferRepository jobOfferRepository;
  private final EmailService emailService;
  private final FraudDetectionService fraudDetectionService;

  // ─── CREATE ──────────────────────────────────────────────────────────────

  @Override
  @Transactional
  public Application create(Application application) {
    Long jobOfferId = application.getJobOfferId();

    // ✅ Récupère l'offre
    JobOffer jobOffer = jobOfferRepository
            .findById(jobOfferId)
            .orElseThrow(() -> new RuntimeException(
                    "Erreur : L'offre avec l'ID " + jobOfferId
                            + " n'existe pas. Impossible de postuler."));

    // ✅ Vérifie si l'offre est active
    if (!jobOffer.getActive()) {
      throw new RuntimeException("Cette offre n'est plus disponible.");
    }

    if (application.getDate() == null) {
      application.setDate(LocalDateTime.now());
    }
    if (application.getStatus() == null) {
      application.setStatus(ApplicationStatus.PENDING);
    }
    if (application.getStudentId() == null) {
      application.setStudentId(0L);
    }
    application.setId(null);

    // ✅ Sauvegarde
    Application saved = repository.save(application);

    // ✅ Analyse fraude
    try {
      FraudResultDto fraudResult = fraudDetectionService.analyze(saved);

      if ("BLOCKED".equals(fraudResult.getFraudLevel())) {
        saved.setStatus(ApplicationStatus.BLOCKED);
        repository.save(saved);
        log.warn("🚨 Candidature bloquée — studentId={}", saved.getStudentId());
        System.out.println("🚨 Candidature BLOQUÉE pour : " + saved.getApplicantEmail());
      } else if ("SUSPICIOUS".equals(fraudResult.getFraudLevel())) {
        log.warn("⚠️ Candidature suspecte — studentId={}", saved.getStudentId());
        System.out.println("⚠️ Candidature SUSPECTE pour : " + saved.getApplicantEmail());
      }

    } catch (Exception e) {
      log.warn("⚠️ Analyse fraude échouée : {}", e.getMessage());
    }

    // ✅ Email confirmation
    try {
      if (saved.getApplicantEmail() != null
              && !saved.getApplicantEmail().isEmpty()
              && saved.getStatus() != ApplicationStatus.BLOCKED) {
        emailService.sendApplicationConfirmation(
                saved.getApplicantEmail(),
                saved.getApplicantName() != null
                        ? saved.getApplicantName() : "Candidat",
                jobOffer.getTitle(),
                jobOffer.getCompany()
        );
      }
    } catch (Exception e) {
      log.warn("⚠️ Email non envoyé : {}", e.getMessage());
    }

    return saved;
  }

  // ─── READ ─────────────────────────────────────────────────────────────────

  @Override
  @Transactional(readOnly = true)
  public List<Application> findAll() {
    return repository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public Application findById(Long id) {
    return repository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Application not found with id=" + id));
  }

  // ─── UPDATE ───────────────────────────────────────────────────────────────

  @Override
  public Application update(Long id, Application updated) {
    Application existing = findById(id);

    ApplicationStatus oldStatus = existing.getStatus();
    ApplicationStatus newStatus = updated.getStatus();

    existing.setJobOfferId(updated.getJobOfferId());
    existing.setStudentId(updated.getStudentId() != null
            ? updated.getStudentId() : existing.getStudentId());
    existing.setApplicantName(updated.getApplicantName());
    existing.setApplicantEmail(updated.getApplicantEmail());
    existing.setCvUrl(updated.getCvUrl());
    existing.setCoverLetterUrl(updated.getCoverLetterUrl());
    existing.setCoverLetter(updated.getCoverLetter());
    existing.setStatus(newStatus != null ? newStatus : existing.getStatus());
    existing.setDate(updated.getDate() != null
            ? updated.getDate() : existing.getDate());

    // ✅ Passage à INTERVIEW → planification Option C
    if (newStatus == ApplicationStatus.INTERVIEW
            && oldStatus != ApplicationStatus.INTERVIEW) {

      LocalDateTime interviewDate;

      if (updated.getInterviewDate() != null) {
        // ✅ Recruteur a proposé une date → vérifie conflit
        boolean hasConflict = repository
                .findByStatus(ApplicationStatus.INTERVIEW)
                .stream()
                .anyMatch(a -> a.getInterviewDate() != null
                        && !a.getId().equals(existing.getId())
                        && Math.abs(Duration.between(
                                a.getInterviewDate(),
                                updated.getInterviewDate())
                        .toMinutes()) < 60);

        if (hasConflict) {
          System.out.println("⚠️ Conflit détecté → planification automatique");
          log.warn("⚠️ Conflit détecté pour {} → planification auto",
                  existing.getApplicantName());
          interviewDate = findNextAvailableSlot();
        } else {
          interviewDate = updated.getInterviewDate();
          System.out.println("✅ Date choisie acceptée : " + interviewDate);
          log.info("✅ Date manuelle acceptée : {}", interviewDate);
        }
      } else {
        // ✅ Pas de date → planification automatique
        interviewDate = findNextAvailableSlot();
        System.out.println("🤖 Date auto assignée : " + interviewDate);
        log.info("🤖 Date auto : {}", interviewDate);
      }

      existing.setInterviewDate(interviewDate);

      System.out.println("📅 Interview planifié pour : "
              + existing.getApplicantName()
              + " → " + interviewDate);

      // ✅ Email invitation entretien
      try {
        JobOffer jobOffer = jobOfferRepository
                .findById(existing.getJobOfferId())
                .orElse(null);

        if (jobOffer != null && existing.getApplicantEmail() != null) {
          emailService.sendInterviewInvitation(
                  existing.getApplicantEmail(),
                  existing.getApplicantName(),
                  jobOffer.getTitle(),
                  jobOffer.getCompany(),
                  interviewDate
          );
          System.out.println("✅ Email entretien envoyé à : "
                  + existing.getApplicantEmail());
        }
      } catch (Exception e) {
        log.warn("⚠️ Email entretien non envoyé : {}", e.getMessage());
      }
    }

    Application saved = repository.save(existing);

    // ✅ Si ACCEPTED → annule les autres candidatures de la même offre
    if (newStatus == ApplicationStatus.ACCEPTED) {
      List<Application> sameOfferApplications =
              repository.findByJobOfferId(existing.getJobOfferId());

      for (Application other : sameOfferApplications) {
        if (!other.getId().equals(saved.getId())
                && other.getStatus() != ApplicationStatus.CANCELLED) {
          other.setStatus(ApplicationStatus.CANCELLED);
        }
      }
      repository.saveAll(sameOfferApplications);
      System.out.println("✅ Autres candidatures annulées pour offre "
              + existing.getJobOfferId());
    }

    return saved;
  }

  // ─── DELETE ───────────────────────────────────────────────────────────────

  @Override
  public void delete(Long id) {
    Application existing = findById(id);
    repository.delete(existing);
  }

  // ─── PLANIFICATION AUTOMATIQUE ────────────────────────────────────────────

  /**
   * Trouve le prochain créneau disponible entre 9h et 15h
   * sans conflit avec les entretiens existants.
   */
  private LocalDateTime findNextAvailableSlot() {
    // ✅ Commence demain à 9h
    LocalDateTime candidate = LocalDateTime.now()
            .plusDays(1)
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

    // ✅ Récupère tous les créneaux déjà occupés
    List<LocalDateTime> bookedSlots = repository
            .findByStatus(ApplicationStatus.INTERVIEW)
            .stream()
            .map(Application::getInterviewDate)
            .filter(d -> d != null)
            .collect(Collectors.toList());

    System.out.println("🔍 Créneaux déjà occupés : " + bookedSlots.size());

    // ✅ Cherche un créneau libre (max 30 jours)
    int maxDays = 30;
    while (maxDays-- > 0) {
      // ✅ Vérifie chaque heure de 9h à 15h
      for (int hour = 9; hour <= 15; hour++) {
        LocalDateTime slot = candidate.withHour(hour);

        boolean isBooked = bookedSlots.stream()
                .anyMatch(booked ->
                        Math.abs(Duration.between(booked, slot)
                                .toMinutes()) < 60);

        if (!isBooked) {
          System.out.println("✅ Créneau libre trouvé : " + slot);
          return slot;
        }
      }

      // ✅ Passe au jour suivant
      candidate = candidate.plusDays(1).withHour(9);
      System.out.println("📅 Essai jour suivant : " + candidate.toLocalDate());
    }

    // ✅ Fallback — demain à 9h
    System.out.println("⚠️ Fallback : demain 9h");
    return LocalDateTime.now().plusDays(1).withHour(9)
            .withMinute(0).withSecond(0).withNano(0);
  }
}