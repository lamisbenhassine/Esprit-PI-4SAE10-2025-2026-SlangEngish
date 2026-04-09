package esprit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Value;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    // Pour Docker : via `docker-compose`, on peut mettre http://user:8011
    // En dev : default = http://localhost:8011
    @Value("${users.base-url:http://localhost:8011}")
    private String usersBaseUrl;
    @Value("${reclamation.base-url:http://localhost:8012}")
    private String reclamationBaseUrl;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User microservice (direct to 8011; use lb://user when Eureka is running)
                .route("user-auth", r -> r.path("/api/auth/**")
                        .uri(usersBaseUrl))
                .route("user-users", r -> r.path("/api/users/**")
                        .uri(usersBaseUrl))
                .route("reclamation-api", r -> r.path("/api/reclamations/**")
                        .uri(reclamationBaseUrl))
                .build();
    }
}
