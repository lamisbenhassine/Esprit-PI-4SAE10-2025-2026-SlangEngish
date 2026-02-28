package com.school.schoolservice.savedoffer.repository;

import com.school.schoolservice.savedoffer.entity.SavedOffer;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface SavedOfferRepository extends JpaRepository<SavedOffer, Long> {

  List<SavedOffer> findByStudentId(Long studentId);

  @Query("SELECT s FROM SavedOffer s JOIN FETCH s.jobOffer WHERE s.studentId = :studentId")
  List<SavedOffer> findByStudentIdWithOffer(Long studentId);
}

