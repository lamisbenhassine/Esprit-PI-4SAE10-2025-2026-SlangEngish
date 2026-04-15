package com.school.schoolservice.joboffer.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.school.schoolservice.application.entity.Application;
import com.school.schoolservice.joboffer.enums.JobType;
import com.school.schoolservice.savedoffer.entity.SavedOffer;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "job_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobOffer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 200)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  private String location;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private JobType contractType;

  @Column(length = 200)
  private String company;

  @Column(length = 100)
  private String salary;

  @Column(name = "recruiter_id")
  private Long recruiterId;

  @Column(nullable = false)
  private LocalDateTime date;

  @Column(nullable = false)
  private Boolean active = true;

  @Column
  private Double latitude;

  @Column
  private Double longitude;

  /** When non-null and in the past, scheduler will deactivate the offer. When null, offer never expires automatically. */
  @Column(name = "expiration_date", nullable = true)
  private LocalDateTime expirationDate;

  @Column(name = "view_count", nullable = false)
  @Builder.Default
  private Long viewCount = 0L;

  @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<Application> applications;

  @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
  @JsonIgnore
  private List<SavedOffer> savedOffers;
}

