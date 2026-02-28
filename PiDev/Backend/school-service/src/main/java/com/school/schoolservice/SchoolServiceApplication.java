package com.school.schoolservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableDiscoveryClient
@EnableScheduling // ✅ ajoute cette annotation

public class SchoolServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(SchoolServiceApplication.class, args);
  }
}

