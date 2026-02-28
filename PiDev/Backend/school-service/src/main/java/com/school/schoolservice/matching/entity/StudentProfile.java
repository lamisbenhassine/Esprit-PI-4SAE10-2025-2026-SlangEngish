package com.school.schoolservice.matching.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_profiles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long studentId;

    @Column
    private String preferredLocation;

    @Column
    private String preferredContractType;

    @Column
    private Double expectedSalary;

    @Column(columnDefinition = "TEXT")
    private String skills;
}
