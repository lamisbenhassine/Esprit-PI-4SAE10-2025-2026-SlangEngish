package esprit.reclamation.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Appelle le service Python (Random Forest) décrit dans ml-classifier/ — la partie SVM du notebook n'est pas utilisée.
 */
@Service
public class ReclamationMlClassifierClient {

    private static final Logger log = LoggerFactory.getLogger(ReclamationMlClassifierClient.class);

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public ReclamationMlClassifierClient(
            @Value("${reclamation.ml.base-url:}") String baseUrl,
            RestTemplate restTemplate
    ) {
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.restTemplate = restTemplate;
    }

    public Optional<String> predictCategory(String sujet, String description) {
        if (this.baseUrl.isEmpty()) {
            return Optional.empty();
        }
        String combined = (safe(sujet) + " " + safe(description)).trim();
        if (combined.isEmpty()) {
            return Optional.empty();
        }
        String url = this.baseUrl.endsWith("/") ? this.baseUrl + "predict" : this.baseUrl + "/predict";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, String> body = new HashMap<>();
        body.put("text", combined);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> resp = this.restTemplate.postForObject(url, new HttpEntity<>(body, headers), Map.class);
            if (resp == null || !resp.containsKey("category")) {
                return Optional.empty();
            }
            Object cat = resp.get("category");
            if (cat == null) {
                return Optional.empty();
            }
            String s = String.valueOf(cat).trim();
            return s.isEmpty() ? Optional.empty() : Optional.of(s);
        } catch (RestClientException e) {
            log.warn("ML classifier HTTP call failed ({}): {}", url, e.getMessage());
            return Optional.empty();
        }
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
