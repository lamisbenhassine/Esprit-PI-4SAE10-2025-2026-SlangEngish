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
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder){
        return builder.routes()
                .route("evaluation", r -> r.path("/evaluation/**").uri("http://localhost:8020/"))
                .route("user", r -> r.path("/user/**").uri("http://localhost:8011/"))
                .route("auth", r -> r.path("/api/auth/**").uri("http://localhost:8011"))
                .route("frontend", r -> r.order(1).path("/**").uri("http://127.0.0.1:4200"))
                .build();
    }
}
