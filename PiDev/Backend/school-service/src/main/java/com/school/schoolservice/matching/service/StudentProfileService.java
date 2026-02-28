package com.school.schoolservice.matching.service;

import com.school.schoolservice.matching.entity.StudentProfile;
import java.util.Optional;

public interface StudentProfileService {
    StudentProfile save(Long studentId, StudentProfile profile);
    Optional<StudentProfile> findByStudentId(Long studentId);
}