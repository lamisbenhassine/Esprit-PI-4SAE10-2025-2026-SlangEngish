package com.school.schoolservice.joboffer.service.impl;

import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.joboffer.dto.NewOfferNotificationDto;
import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import com.school.schoolservice.joboffer.service.JobOfferService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class JobOfferServiceImpl implements JobOfferService {

  public static final String TOPIC_NEW_OFFERS = "/topic/new-offers";

  private final JobOfferRepository repository;
  private final SimpMessagingTemplate messagingTemplate;

  @Override
  public JobOffer create(JobOffer jobOffer) {
    if (jobOffer.getDate() == null) {
      jobOffer.setDate(LocalDateTime.now());
    }
    if (jobOffer.getRecruiterId() == null) {
      jobOffer.setRecruiterId(1L);
    }
    if (jobOffer.getActive() == null) {
      jobOffer.setActive(true);
    }
    // expirationDate: do not set any default; keep exactly what is passed (null = never expire).
    jobOffer.setId(null);
    JobOffer created = repository.save(jobOffer);
    notifyFrontOfficeNewOffer(created);
    return created;
  }

  private void notifyFrontOfficeNewOffer(JobOffer offer) {
    NewOfferNotificationDto dto = NewOfferNotificationDto.builder()
        .id(offer.getId())
        .title(offer.getTitle())
        .company(offer.getCompany())
        .location(offer.getLocation())
        .contractType(offer.getContractType() != null ? offer.getContractType().name() : null)
        .date(offer.getDate())
        .message("Nouvelle offre publiée : " + (offer.getTitle() != null ? offer.getTitle() : ""))
        .build();
    messagingTemplate.convertAndSend(TOPIC_NEW_OFFERS, dto);
  }

  @Override
  @Transactional(readOnly = true)
  public List<JobOffer> findAll() {
    return repository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public JobOffer findById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("JobOffer not found with id=" + id));
  }

  @Override
  public JobOffer update(Long id, JobOffer updated) {

    JobOffer existing = findById(id);
    existing.setTitle(updated.getTitle());
    existing.setDescription(updated.getDescription());
    existing.setLocation(updated.getLocation());
    existing.setContractType(updated.getContractType());
    existing.setCompany(updated.getCompany());
    existing.setSalary(updated.getSalary());
    existing.setRecruiterId(updated.getRecruiterId() != null ? updated.getRecruiterId() : existing.getRecruiterId());
    existing.setDate(updated.getDate() != null ? updated.getDate() : existing.getDate());
    existing.setActive(updated.getActive() != null ? updated.getActive() : existing.getActive());
    existing.setExpirationDate(updated.getExpirationDate());

    return repository.save(existing);
  }

  @Override
  public void delete(Long id) {
    JobOffer existing = findById(id);
    repository.delete(existing);
  }

  @Override
  public JobOffer incrementViewCount(Long id) {
    JobOffer offer = findById(id);
    offer.setViewCount(offer.getViewCount() == null ? 1L : offer.getViewCount() + 1);
    return repository.save(offer);
  }
}

