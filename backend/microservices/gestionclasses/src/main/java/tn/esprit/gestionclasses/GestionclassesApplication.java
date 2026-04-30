package tn.esprit.gestionclasses;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class GestionclassesApplication {

    public static void main(String[] args) {
        SpringApplication.run(GestionclassesApplication.class, args);
    }

}
