package esprit.inscription.client;

import esprit.inscription.entity.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * OpenFeign client for communicating with the User microservice.
 *
 * The User microservice is registered in Eureka with spring.application.name=user
 * and is exposed through the gateway on /api/user/**.
 *
 * When the REST controller for the user service is added, it should expose:
 *   GET /api/user/{id}
 * returning a User (or compatible DTO), so this client can consume it.
 */
@FeignClient(name = "user", path = "/api/user")
public interface UserClient {

    @GetMapping("/{id}")
    User getUserById(@PathVariable("id") Long id);
}

