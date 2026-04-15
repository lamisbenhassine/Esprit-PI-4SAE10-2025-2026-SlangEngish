package com.school.schoolservice.application.repository;

import com.school.schoolservice.application.entity.Application;

import java.time.LocalDateTime;
import java.util.List;

import com.school.schoolservice.application.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;


public interface ApplicationRepository extends JpaRepository<Application, Long> {

  List<Application> findByJobOfferId(Long jobOfferId);

  List<Application> findByStudentId(Long studentId);
  long countByJobOfferId(Long jobOfferId);

  // ✅ Compte par statut
  @Query("SELECT a.status, COUNT(a) FROM Application a GROUP BY a.status")
  List<Object[]> countByStatus();

  // ✅ Offre avec le plus de candidatures
  @Query("SELECT a.jobOfferId, COUNT(a) as cnt FROM Application a GROUP BY a.jobOfferId ORDER BY cnt DESC")
  List<Object[]> countByJobOfferIdOrderByCount();

  // ✅ Pour le scheduler
  List<Application> findByStatusAndInterviewDateBetween(
          ApplicationStatus status,
          LocalDateTime start,
          LocalDateTime end
  );

  List<Application> findByStatus(ApplicationStatus status);
}

