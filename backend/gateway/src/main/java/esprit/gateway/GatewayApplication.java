package esprit.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Microservice Users (port 8011) - Auth et Users
                .route("users-auth", r -> r.path("/api/auth/**")
                        .uri("http://localhost:8011"))
                .route("users-api", r -> r.path("/api/users/**")
                        .uri("http://localhost:8011"))
                // Microservice Evenement (port 8088) - Événements, Inscriptions, Invitations
                .route("evenement-api", r -> r.path("/api/evenements/**")
                        .uri("http://localhost:8088"))
                .route("inscriptions-api", r -> r.path("/api/inscriptions/**")
                        .uri("http://localhost:8088"))
                .route("invitation-api", r -> r.path("/api/invitation/**")
                        .uri("http://localhost:8088"))
                .route("feedback-api", r -> r.path("/api/feedback/**")
                        .uri("http://localhost:8088"))
                // Microservice Club (port 8087)
                .route("club-api", r -> r.path("/api/clubs/**")
                        .uri("http://localhost:8087"))
                .route("club-reunion-api", r -> r.path("/api/reunions-club/**")
                        .uri("http://localhost:8087"))
                .route("club-post-api", r -> r.path("/api/posts-club/**")
                        .uri("http://localhost:8087"))
                // Evaluation (port 8020) - si utilisé
                .route("evaluation", r -> r.path("/evaluation/**")
                        .uri("http://localhost:8020"))
                .build();
    }
}
