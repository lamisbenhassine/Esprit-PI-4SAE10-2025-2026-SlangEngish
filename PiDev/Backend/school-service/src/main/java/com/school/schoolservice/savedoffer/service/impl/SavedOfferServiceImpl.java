package com.school.schoolservice.savedoffer.service.impl;

import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.savedoffer.entity.SavedOffer;
import com.school.schoolservice.savedoffer.repository.SavedOfferRepository;
import com.school.schoolservice.savedoffer.service.SavedOfferService;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SavedOfferServiceImpl implements SavedOfferService {

  private final SavedOfferRepository repository;

  @Override
  public SavedOffer create(SavedOffer savedOffer) {
    if (savedOffer.getSavedAt() == null) {
      savedOffer.setSavedAt(LocalDateTime.now());
    }
    savedOffer.setId(null);
    return repository.save(savedOffer);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedOffer> findAll() {
    return repository.findAll();
  }

  @Override
  @Transactional(readOnly = true)
  public SavedOffer findById(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SavedOffer not found with id=" + id));
  }

  @Override
  public SavedOffer update(Long id, SavedOffer updated) {
    SavedOffer existing = findById(id);
    existing.setJobOfferId(updated.getJobOfferId());
    existing.setStudentId(updated.getStudentId());
    existing.setSavedAt(updated.getSavedAt() != null ? updated.getSavedAt() : existing.getSavedAt());
    existing.setNotes(updated.getNotes());
    return repository.save(existing);
  }

  @Override
  public void delete(Long id) {
    SavedOffer existing = findById(id);
    repository.delete(existing);
  }

  @Override
  public void remove(Long id) {
    delete(id);
  }

  @Override
  @Transactional(readOnly = true)
  public List<SavedOffer> findByStudentId(Long studentId) {
    if (studentId == null) {
      return List.of();
    }
    return repository.findByStudentId(studentId);
  }
}

