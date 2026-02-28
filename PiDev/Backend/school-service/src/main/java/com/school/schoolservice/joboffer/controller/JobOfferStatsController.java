package com.school.schoolservice.joboffer.controller;


import com.school.schoolservice.joboffer.dto.JobOfferStatsDto;
import com.school.schoolservice.joboffer.service.JobOfferStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/joboffers-stats")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JobOfferStatsController {

    private final JobOfferStatsService service;

    @GetMapping
    public ResponseEntity<JobOfferStatsDto> getStats() {
        return ResponseEntity.ok(service.getStats());
    }
}