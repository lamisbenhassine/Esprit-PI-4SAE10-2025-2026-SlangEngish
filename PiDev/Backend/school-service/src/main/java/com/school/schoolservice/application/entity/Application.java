  package com.school.schoolservice.application.entity;

  import com.fasterxml.jackson.annotation.JsonIgnore;
  import com.school.schoolservice.application.enums.ApplicationStatus;
  import com.school.schoolservice.joboffer.entity.JobOffer;
  import jakarta.persistence.Column;
  import jakarta.persistence.Entity;
  import jakarta.persistence.EnumType;
  import jakarta.persistence.Enumerated;
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
  @Table(name = "applications")
  @Getter
  @Setter
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_offer_id", nullable = false)
    private Long jobOfferId;

    @Column(name = "student_id")
    private Long studentId;

    @Column(name = "applicant_name", length = 200)
    private String applicantName;

    @Column(name = "applicant_email", length = 255)
    private String applicantEmail;

    @Column(name = "cv_url", length = 500)
    private String cvUrl;

    @Column(name = "cover_letter_url", length = 500)
    private String coverLetterUrl;

    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, columnDefinition = "varchar(50) not null")
    private ApplicationStatus status;

    @Column(nullable = false)
    private LocalDateTime date;

    @Column(name = "interview_date")
    private LocalDateTime interviewDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", insertable = false, updatable = false)
    private JobOffer jobOffer;
  }

