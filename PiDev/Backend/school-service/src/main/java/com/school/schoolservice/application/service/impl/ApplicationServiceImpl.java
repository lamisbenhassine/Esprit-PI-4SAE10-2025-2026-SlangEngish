package com.school.schoolservice.application.service.impl;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.enums.ApplicationStatus;
import com.school.schoolservice.application.repository.ApplicationRepository;
import com.school.schoolservice.application.service.ApplicationService;
import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ApplicationServiceImpl implements ApplicationService {

  private final ApplicationRepository repository;
  private final JobOfferRepository jobOfferRepository;

  @Override
  @Transactional
  public Application create(Application application) {
    Long jobOfferId = application.getJobOfferId();
    if (!jobOfferRepository.existsById(jobOfferId)) {
      throw new RuntimeException(
          "Erreur : L'offre avec l'ID " + jobOfferId + " n'existe pas. Impossible de postuler.");
    }

    if (application.getDate() == null) {
      application.setDate(LocalDateTime.now());
    }
    if (application.getStatus() == null) {
      application.setStatus(ApplicationStatus.PENDING);
    }
    if (application.getStudentId() == null) {
      application.setStudentId(1L);
    }
    application.setId(null);
    return repository.save(application);
  }

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
        .orElseThrow(() -> new ResourceNotFoundException("Application not found with id=" + id));
  }

  @Override
  public Application update(Long id, Application updated) {
    Application existing = findById(id);

    existing.setJobOfferId(updated.getJobOfferId());
    existing.setStudentId(updated.getStudentId() != null ? updated.getStudentId() : existing.getStudentId());
    existing.setApplicantName(updated.getApplicantName());
    existing.setApplicantEmail(updated.getApplicantEmail());
    existing.setCvUrl(updated.getCvUrl());
    existing.setCoverLetterUrl(updated.getCoverLetterUrl());
    existing.setCoverLetter(updated.getCoverLetter());
    existing.setStatus(updated.getStatus() != null ? updated.getStatus() : existing.getStatus());
    existing.setDate(updated.getDate() != null ? updated.getDate() : existing.getDate());

    Application saved = repository.save(existing);

    if (updated.getStatus() == ApplicationStatus.ACCEPTED) {
      List<Application> sameStudentApplications =
          repository.findByStudentId(existing.getStudentId());

      for (Application other : sameStudentApplications) {
        if (!other.getId().equals(saved.getId())
            && other.getStatus() != ApplicationStatus.CANCELLED) {
          other.setStatus(ApplicationStatus.CANCELLED);
        }
      }

      repository.saveAll(sameStudentApplications);
    }

    return saved;
  }

  @Override
  public void delete(Long id) {
    Application existing = findById(id);
    repository.delete(existing);
  }
}

