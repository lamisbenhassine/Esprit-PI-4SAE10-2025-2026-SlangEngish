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
    @Value("${evaluation.base-url:http://localhost:8020}")
    private String evaluationBaseUrl;
    @Value("${notebook.base-url:http://localhost:8030}")
    private String notebookBaseUrl;
    @Value("${cours.base-url:http://localhost:9090}")
    private String coursBaseUrl;
    @Value("${gestionclasses.base-url:http://localhost:9092}")
    private String gestionclassesBaseUrl;
    @Value("${forum.base-url:http://localhost:8040}")
    private String forumBaseUrl;
    @Value("${inscription.base-url:http://localhost:8050}")
    private String inscriptionBaseUrl;

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
                // Evaluation microservice
                .route("evaluation-api", r -> r.path("/api/evaluation/**")
                        .filters(f -> f.stripPrefix(1)
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(evaluationBaseUrl))
                // Public uploads (served by evaluation service at /uploads/**)
                .route("evaluation-uploads", r -> r.path("/api/uploads/**")
                        .filters(f -> f.stripPrefix(1)
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(evaluationBaseUrl))
                // Smart Notebook microservice
                .route("notebook-api", r -> r.path("/api/notebook/**")
                        .filters(f -> f.stripPrefix(1)
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(notebookBaseUrl))
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
                // Cours microservice (mixed prefixes: some controllers are /api/*, others are not)
                .route("cours-api-prefixed", r -> r.path("/api/chapters/**", "/api/recordings/**")
                        .filters(f -> f.prefixPath("/pidev4sae10")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(coursBaseUrl))
                .route("cours-api", r -> r.path(
                                "/api/course/**",
                                "/api/chapter/**",
                                "/api/recording/**",
                                "/api/chapter-note/**",
                                "/api/notifications/**",
                                "/api/progression/**",
                                "/api/stream/**")
                        // downstream controllers are not prefixed with /api -> remove it
                        .filters(f -> f.stripPrefix(1)
                                .prefixPath("/pidev4sae10")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(coursBaseUrl))
                // Gestion classes microservice (controllers are not prefixed with /api)
                .route("gestionclasses-api", r -> r.path("/api/kanban/**", "/api/professor-availability/**")
                        .filters(f -> f.stripPrefix(1)
                                .prefixPath("/pidev4sae10")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(gestionclassesBaseUrl))
                // Forum microservice (paths: /api/forum/**)
                .route("forum-api", r -> r.path("/api/forum/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(forumBaseUrl))
                // Inscription microservice (singular /api/inscription/** — distinct from evenement /api/inscriptions/**)
                .route("inscription-api", r -> r.path("/api/inscription/**")
                        .filters(f -> f.dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST"))
                        .uri(inscriptionBaseUrl))
                .build();
    }
}
