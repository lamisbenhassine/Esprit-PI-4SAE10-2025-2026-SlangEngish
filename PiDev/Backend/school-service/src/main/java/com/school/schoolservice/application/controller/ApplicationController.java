package com.school.schoolservice.application.controller;

import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.application.service.ApplicationService;
import com.school.schoolservice.common.exception.ResourceNotFoundException;
import com.school.schoolservice.joboffer.repository.JobOfferRepository;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApplicationController {

  private final ApplicationService service;
  private final JobOfferRepository jobOfferRepository;

  /**
   * New creation route using jobOfferId from URL.
   * POST /api/job-offers/{jobOfferId}/applications
   */
  @PostMapping("/job-offers/{jobOfferId}/applications")
  public ResponseEntity<Application> createForJobOffer(
      @PathVariable Long jobOfferId, @RequestBody Application application) {

    if (!jobOfferRepository.existsById(jobOfferId)) {
      throw new ResourceNotFoundException("JobOffer not found with id=" + jobOfferId);
    }

    application.setJobOfferId(jobOfferId);
    Application created = service.create(application);
    return ResponseEntity.created(URI.create("/api/applications/" + created.getId())).body(created);
  }

  // Existing CRUD endpoints remain, now under /api/applications...

  @PostMapping("/applications")
  public ResponseEntity<Application> create(@RequestBody Application application) {
    Application created = service.create(application);
    return ResponseEntity.created(URI.create("/api/applications/" + created.getId())).body(created);
  }

  @GetMapping("/applications")
  public ResponseEntity<List<Application>> findAll() {
    return ResponseEntity.ok(service.findAll());
  }

  @GetMapping("/applications/{id}")
  public ResponseEntity<Application> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PutMapping("/applications/{id}")
  public ResponseEntity<Application> update(@PathVariable Long id, @RequestBody Application updated) {
    return ResponseEntity.ok(service.update(id, updated));
  }

  @DeleteMapping("/applications/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}

