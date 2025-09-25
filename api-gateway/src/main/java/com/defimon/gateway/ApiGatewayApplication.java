package com.defimon.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Blockchain data routes
                .route("blockchain-data", r -> r.path("/api/blockchain/**")
                        .filters(f -> f.stripPrefix(2)
                                .circuitBreaker(config -> config.setName("blockchain-cb")))
                        .uri("http://blockchain-sync:8081"))
                
                // Transaction signing routes
                .route("transaction-signing", r -> r.path("/api/transactions/**")
                        .filters(f -> f.stripPrefix(2)
                                .circuitBreaker(config -> config.setName("transaction-cb")))
                        .uri("http://transaction-signing:8082"))
                
                // Math computing routes
                .route("math-computing", r -> r.path("/api/calculations/**")
                        .filters(f -> f.stripPrefix(2)
                                .circuitBreaker(config -> config.setName("math-cb")))
                        .uri("http://math-computing:8083"))
                
                // Data aggregation routes
                .route("data-aggregation", r -> r.path("/api/data/**")
                        .filters(f -> f.stripPrefix(2)
                                .circuitBreaker(config -> config.setName("data-cb")))
                        .uri("http://data-aggregation:8084"))
                
                // Health check routes
                .route("health", r -> r.path("/health")
                        .uri("http://localhost:8080/actuator/health"))
                
                .build();
    }

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.addAllowedOrigin("*");
        corsConfig.addAllowedMethod("*");
        corsConfig.addAllowedHeader("*");
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
