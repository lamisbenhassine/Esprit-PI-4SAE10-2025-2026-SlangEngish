package com.school.schoolservice.matching.service.impl;

import com.school.schoolservice.matching.entity.StudentProfile;
import com.school.schoolservice.matching.repository.StudentProfileRepository;
import com.school.schoolservice.matching.service.StudentProfileService;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class StudentProfileServiceImpl implements StudentProfileService {

    private final StudentProfileRepository repository;

    @Override
    public StudentProfile save(Long studentId, StudentProfile profile) {
        Optional<StudentProfile> existing = repository.findByStudentId(studentId);
        if (existing.isPresent()) {
            StudentProfile p = existing.get();
            p.setPreferredLocation(profile.getPreferredLocation());
            p.setPreferredContractType(profile.getPreferredContractType());
            p.setExpectedSalary(profile.getExpectedSalary());
            p.setSkills(profile.getSkills());
            return repository.save(p);
        }
        profile.setStudentId(studentId);
        return repository.save(profile);
    }

    @Override
    public Optional<StudentProfile> findByStudentId(Long studentId) {
        return repository.findByStudentId(studentId);
    }
}