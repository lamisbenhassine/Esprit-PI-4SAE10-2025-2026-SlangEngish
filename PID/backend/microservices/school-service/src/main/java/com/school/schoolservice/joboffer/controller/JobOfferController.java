package com.school.schoolservice.joboffer.controller;

import com.school.schoolservice.joboffer.entity.JobOffer;
import com.school.schoolservice.joboffer.service.JobOfferService;
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
@RequestMapping("/api/joboffers")
@RequiredArgsConstructor
public class JobOfferController {

  private final JobOfferService service;

  @PostMapping
  public ResponseEntity<JobOffer> create(@RequestBody JobOffer jobOffer) {
    JobOffer created = service.create(jobOffer);
    return ResponseEntity.created(URI.create("/api/joboffers/" + created.getId())).body(created);
  }

  @GetMapping
  public ResponseEntity<List<JobOffer>> findAll() {
    return ResponseEntity.ok(service.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<JobOffer> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<JobOffer> update(@PathVariable Long id, @RequestBody JobOffer updated) {
    return ResponseEntity.ok(service.update(id, updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  // ✅ Incrémente les vues
  @PostMapping("/{id}/view")
  public ResponseEntity<Void> incrementView(@PathVariable Long id) {
    service.incrementViewCount(id);
    return ResponseEntity.ok().build();
  }

  // ✅ Change l'URL pour éviter le conflit avec /{id}
  @GetMapping("/views/top")
  public ResponseEntity<List<JobOffer>> getViewStats() {
    return ResponseEntity.ok(
            service.findAll().stream()
                    .filter(o -> o.getActive() != null && o.getActive())
                    .sorted((a, b) -> Long.compare(
                            b.getViewCount() == null ? 0 : b.getViewCount(),
                            a.getViewCount() == null ? 0 : a.getViewCount()))
                    .collect(java.util.stream.Collectors.toList())
    );
  }
}

