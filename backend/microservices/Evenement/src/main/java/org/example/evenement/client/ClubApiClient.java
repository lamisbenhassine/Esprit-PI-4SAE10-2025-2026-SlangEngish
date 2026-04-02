package org.example.evenement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ClubApiClient {

    private final RestTemplate restTemplate;
    private final String clubServiceUrl;

    public ClubApiClient(RestTemplate restTemplate,
                         @Value("${app.club-service.url:http://localhost:8087}") String clubServiceUrl) {
        this.restTemplate = restTemplate;
        this.clubServiceUrl = clubServiceUrl;
    }

    public ClubInfoDto getClubById(Long clubId) {
        if (clubId == null) {
            return null;
        }
        try {
            return restTemplate.getForObject(clubServiceUrl + "/api/clubs/" + clubId, ClubInfoDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
