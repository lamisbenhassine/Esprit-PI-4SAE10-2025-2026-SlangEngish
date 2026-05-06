package com.school.schoolservice.salary.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.schoolservice.salary.dto.SalaryPredictionDto;
import com.school.schoolservice.salary.service.SalaryPredictionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SalaryPredictionServiceImpl
        implements SalaryPredictionService {

    private final ObjectMapper objectMapper;

    @Value("${salary.api.url:http://localhost:5000}")
    private String salaryApiUrl;

    @Override
    public SalaryPredictionDto predictSalary(String jobTitle) {
        try {
            // ✅ Prépare la requête
            Map<String, String> body = new HashMap<>();
            body.put("jobTitle", jobTitle);

            String jsonBody = objectMapper
                    .writeValueAsString(body);

            // ✅ Appelle l'API Flask
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(salaryApiUrl + "/predict"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers
                            .ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString());

            // ✅ Parse la réponse
            SalaryPredictionDto result = objectMapper
                    .readValue(response.body(),
                            SalaryPredictionDto.class);

            System.out.println("💰 Prédiction pour '"
                    + jobTitle + "' : "
                    + result.getSalaryMonthlyTND() + " TND");

            return result;

        } catch (Exception e) {
            log.error("Salary prediction error: {}",
                    e.getMessage());

            // ✅ Retourne une prédiction par défaut
            return SalaryPredictionDto.builder()
                    .jobTitle(jobTitle)
                    .salaryMonthlyTND(2500)
                    .salaryAnnualTND(30000)
                    .salaryRangeLow(2000)
                    .salaryRangeHigh(3000)
                    .confidence(50)
                    .currency("TND")
                    .status("fallback")
                    .message("AI prediction unavailable, "
                            + "showing estimated salary")
                    .build();
        }
    }
}