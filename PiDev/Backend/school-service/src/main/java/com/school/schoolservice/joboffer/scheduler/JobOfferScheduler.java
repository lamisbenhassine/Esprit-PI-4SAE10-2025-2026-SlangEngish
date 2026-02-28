package com.school.schoolservice.joboffer.scheduler;

import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobOfferScheduler {

  private final JobOfferRepository jobOfferRepository;

  /** Runs every minute. Deactivates only offers that have expirationDate set and in the past. */
  @Scheduled(fixedRate = 60000)
  @Transactional
  public void deactivateExpiredOffers() {
    LocalDateTime now = LocalDateTime.now();
    List<JobOffer> activeOffers = jobOfferRepository.findByActiveTrue();

    int count = 0;
    for (JobOffer offer : activeOffers) {
      if (offer.getExpirationDate() != null && offer.getExpirationDate().isBefore(now)) {
        offer.setActive(false);
        jobOfferRepository.save(offer);
        count++;
        log.info("Offre expirée désactivée : id={}, title={}", offer.getId(), offer.getTitle());
      }
    }

    if (count > 0) {
      log.info("{} offre(s) désactivée(s) (expiration dépassée)", count);
    }
  }
}
