package esprit.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    @Value("${gateway.gestioncours.uri:lb://GestionCours}")
    private String gestioncoursUri;

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                // Route /api/** to the evaluation microservice (discovered via Eureka as "evaluation")
                .route("evaluation", r -> r.path("/api/**")
                        .uri("lb://evaluation"))
                // Route /cours/** to GestionCours (URI from config: direct URL or lb://GestionCours)
                .route("gestioncours", r -> r.path("/cours/**")
                        .filters(f -> f.rewritePath("/cours(?<segment>/?.*)", "/pidev4sae10${segment}"))
                        .uri(gestioncoursUri))
                .build();
         }
}
