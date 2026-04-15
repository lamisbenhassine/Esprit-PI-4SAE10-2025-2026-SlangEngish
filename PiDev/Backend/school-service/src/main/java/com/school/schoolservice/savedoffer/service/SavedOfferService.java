package com.school.schoolservice.savedoffer.service;

import com.school.schoolservice.savedoffer.entity.SavedOffer;
import java.util.List;

public interface SavedOfferService {
  SavedOffer create(SavedOffer savedOffer);

  List<SavedOffer> findAll();

  SavedOffer findById(Long id);

  SavedOffer update(Long id, SavedOffer updated);

  void delete(Long id);

  void remove(Long id);

  List<SavedOffer> findByStudentId(Long studentId);
}

