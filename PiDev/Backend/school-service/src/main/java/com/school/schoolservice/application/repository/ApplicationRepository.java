package com.school.schoolservice.application.repository;

import com.school.schoolservice.application.entity.Application;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationRepository extends JpaRepository<Application, Long> {

  List<Application> findByJobOfferId(Long jobOfferId);

  List<Application> findByStudentId(Long studentId);
}

