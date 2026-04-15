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
    @Value("${club.base-url:http://localhost:8087}")
    private String clubBaseUrl;
    @Value("${evenement.base-url:http://localhost:8088}")
    private String evenementBaseUrl;
    @Value("${school.base-url:http://localhost:8081}")
    private String schoolBaseUrl;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // User microservice (direct to 8011; use lb://user when Eureka is running)
                .route("user-auth", r -> r.path("/api/auth/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(usersBaseUrl))
                .route("user-users", r -> r.path("/api/users/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(usersBaseUrl))
                .route("reclamation-api", r -> r.path("/api/reclamations/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(reclamationBaseUrl))
                // Club microservice
                .route("club-api", r -> r.path("/api/clubs/**", "/api/posts-club/**", "/api/reunions-club/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(clubBaseUrl))
                // Evenement microservice
                .route("evenement-api", r -> r.path("/api/evenements/**", "/api/inscriptions/**", "/api/feedback/**", "/api/invitation/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(evenementBaseUrl))
                // School microservice (jobs/applications/matching/fraud/chatbot/quiz/files)
                .route("school-api", r -> r.path(
                                "/api/joboffers",
                                "/api/joboffers/**",
                                "/api/job-offers",
                                "/api/job-offers/**",
                                "/api/joboffers-stats",
                                "/api/joboffers-stats/**",
                                "/api/applications",
                                "/api/applications/**",
                                "/api/saved-offers",
                                "/api/saved-offers/**",
                                "/api/matching",
                                "/api/matching/**",
                                "/api/fraud",
                                "/api/fraud/**",
                                "/api/similarity",
                                "/api/similarity/**",
                                "/api/chatbot",
                                "/api/chatbot/**",
                                "/api/files",
                                "/api/files/**",
                                "/api/quiz",
                                "/api/quiz/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(schoolBaseUrl))
                .build();
    }
}
