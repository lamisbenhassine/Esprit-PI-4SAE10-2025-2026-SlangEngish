package com.school.schoolservice.savedoffer.controller;

import com.school.schoolservice.savedoffer.entity.SavedOffer;
import com.school.schoolservice.savedoffer.service.SavedOfferService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/saved-offers")
@RequiredArgsConstructor
public class SavedOfferController {

  private final SavedOfferService service;

  @PostMapping
  public ResponseEntity<SavedOffer> create(@RequestBody SavedOffer savedOffer) {
    SavedOffer created = service.create(savedOffer);
    return ResponseEntity.created(URI.create("/api/saved-offers/" + created.getId())).body(created);
  }

  @GetMapping
  public ResponseEntity<List<SavedOffer>> findAll(
      @RequestParam(value = "studentId", required = false) Long studentId) {
    if (studentId != null) {
      return ResponseEntity.ok(service.findByStudentId(studentId));
    }
    return ResponseEntity.ok(service.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<SavedOffer> findById(@PathVariable Long id) {
    return ResponseEntity.ok(service.findById(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<SavedOffer> update(@PathVariable Long id, @RequestBody SavedOffer updated) {
    return ResponseEntity.ok(service.update(id, updated));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable Long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{id}/remove")
  public ResponseEntity<Void> remove(@PathVariable Long id) {
    service.remove(id);
    return ResponseEntity.noContent().build();
  }
}

