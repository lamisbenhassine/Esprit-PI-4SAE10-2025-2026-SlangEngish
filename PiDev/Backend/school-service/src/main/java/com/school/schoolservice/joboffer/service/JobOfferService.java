package com.school.schoolservice.joboffer.service;

import com.school.schoolservice.joboffer.entity.JobOffer;
import java.util.List;

public interface JobOfferService {
  JobOffer create(JobOffer jobOffer);

  List<JobOffer> findAll();

  JobOffer findById(Long id);

  JobOffer update(Long id, JobOffer updated);

  void delete(Long id);
  JobOffer incrementViewCount(Long id);
}

