package esprit.forum.client;

import esprit.forum.client.dto.UserProfileDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Appelle le microservice user. Sans URL explicite, Feign tente la découverte Eureka (souvent absente en local).
 * {@code user.api.base-url} pointe par défaut vers le user-service (8011) en local.
 */
@FeignClient(name = "user", url = "${user.api.base-url:http://localhost:8011}")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    UserProfileDto getUser(@PathVariable("id") Long id);
}
