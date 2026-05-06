package com.school.schoolservice.application.service;

import com.school.schoolservice.application.entity.Application;
import java.util.List;

public interface ApplicationService {
  Application create(Application application);

  List<Application> findAll();

  Application findById(Long id);

  Application update(Long id, Application updated);

  void delete(Long id);
}

