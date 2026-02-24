package com.school.schoolservice.savedoffer.repository;

import com.school.schoolservice.savedoffer.entity.SavedOffer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavedOfferRepository extends JpaRepository<SavedOffer, Long> {

  List<SavedOffer> findByStudentId(Long studentId);
}

