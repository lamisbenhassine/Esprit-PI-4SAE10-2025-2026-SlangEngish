package com.school.schoolservice.savedoffer.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.schoolservice.joboffer.entity.JobOffer;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "saved_offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedOffer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "job_offer_id", nullable = false)
  private Long jobOfferId;

  @Column(name = "student_id", nullable = false)
  private Long studentId;

  @Column(name = "saved_at", nullable = false)
  private LocalDateTime savedAt;

  @Column(columnDefinition = "TEXT")
  private String notes;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "job_offer_id", insertable = false, updatable = false)
  @JsonIgnoreProperties({"applications", "hibernateLazyInitializer"})

  private JobOffer jobOffer;
}

