package org.example.evenement.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class UserApiClient {

    private final RestTemplate restTemplate;
    private final String userServiceUrl;

    public UserApiClient(RestTemplate restTemplate,
                         @Value("${app.user-service.url:http://localhost:8011}") String userServiceUrl) {
        this.restTemplate = restTemplate;
        this.userServiceUrl = userServiceUrl;
    }

    public UserInfoDto getUserById(Long userId) {
        try {
            return restTemplate.getForObject(userServiceUrl + "/api/users/" + userId, UserInfoDto.class);
        } catch (Exception e) {
            return null;
        }
    }
}
