package org.example.club.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Agrège le nombre d'événements par club (idClub) depuis le microservice événements.
 */
@Component
public class EvenementApiClient {

    private final RestTemplate restTemplate;

    @Value("${app.evenement-service.enabled:true}")
    private boolean enabled;

    @Value("${app.evenement-service.url:http://127.0.0.1:8088}")
    private String evenementServiceUrl;

    public EvenementApiClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public Map<Long, Long> countEvenementsParClub() {
        if (!enabled) {
            return Collections.emptyMap();
        }
        String url = evenementServiceUrl.replaceAll("/$", "") + "/api/evenements";
        try {
            ResponseEntity<List<Map<String, Object>>> resp = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {});
            List<Map<String, Object>> list = resp.getBody();
            if (list == null) {
                return Collections.emptyMap();
            }
            Map<Long, Long> out = new HashMap<>();
            for (Map<String, Object> row : list) {
                Object idClub = row.get("idClub");
                if (idClub instanceof Number n) {
                    long cid = n.longValue();
                    if (cid > 0) {
                        out.merge(cid, 1L, Long::sum);
                    }
                }
            }
            return out;
        } catch (RestClientException e) {
            return Collections.emptyMap();
        }
    }
}
