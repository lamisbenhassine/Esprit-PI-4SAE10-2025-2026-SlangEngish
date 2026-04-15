package com.school.schoolservice.application.controller;

import com.school.schoolservice.application.dto.CvAnalysisResultDto;
import com.school.schoolservice.application.dto.CvScreeningRequestDto;
import com.school.schoolservice.application.dto.CvScreeningResultDto;
import com.school.schoolservice.application.dto.PlagiatResultDto;
import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.service.ApplicationService;
import com.school.schoolservice.application.service.CvAnalysisService;
import com.school.schoolservice.application.service.CvScreeningService;
import com.school.schoolservice.application.service.PlagiatService;
import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ApplicationController {

  private final ApplicationService service;
  private final JobOfferRepository jobOfferRepository;
  private final CvAnalysisService cvAnalysisService;
  private final CvScreeningService cvScreeningService;
  private final PlagiatService plagiatService;

  // ─── CREATE ───────────────────────────────────

  @PostMapping("/job-offers/{jobOfferId}/applications")
  public ResponseEntity<Application> createForJobOffer(
          @PathVariable Long jobOfferId,
          @RequestBody Application application) {
    if (!jobOfferRepository.existsById(jobOfferId)) {
      throw new ResourceNotFoundException(
              "JobOffer not found with id=" + jobOfferId);
    }
    application.setJobOfferId(jobOfferId);
    Application created = service.create(application);
    return ResponseEntity
            .created(URI.create("/api/applications/" + created.getId()))
            .body(created);
  }

  @PostMapping("/applications")
  public ResponseEntity<Application> create(
          @RequestBody Application application) {
    Application created = service.create(application);
    return ResponseEntity
            .created(URI.create("/api/applications/" + created.getId()))
            .body(created);
  }

  // ─── READ ALL ─────────────────────────────────

  @GetMapping("/applications")
  public ResponseEntity<List<Application>> findAll() {
    return ResponseEntity.ok(service.findAll());
  }

  // ─── ✅ ENDPOINTS SPÉCIAUX AVANT /{id} ────────

  @PostMapping("/applications/screening")
  public ResponseEntity<List<CvScreeningResultDto>> screenCVs(
          @RequestBody CvScreeningRequestDto request) {
    return ResponseEntity.ok(cvScreeningService.screenCVs(request));
  }

  @GetMapping("/applications/plagiat")
  public ResponseEntity<List<PlagiatResultDto>> detectPlagiat(
          @RequestParam(required = false) Long jobOfferId) {
    return ResponseEntity.ok(plagiatService.detectPlagiat(jobOfferId));
  }

  // ─── /{id} APRÈS les endpoints spéciaux ───────

  @GetMapping("/applications/{id}")
  public ResponseEntity<Application> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @GetMapping("/applications/{id}/analyze-cv")
  public ResponseEntity<CvAnalysisResultDto> analyzeCv(
          @PathVariable Long id) {
    return ResponseEntity.ok(cvAnalysisService.analyzeCV(id));
  }

  @PutMapping("/applications/{id}")
  public ResponseEntity<Application> update(
          @PathVariable Long id,
          @RequestBody Application updated) {
    return ResponseEntity.ok(service.update(id, updated));
  }

  @DeleteMapping("/applications/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}