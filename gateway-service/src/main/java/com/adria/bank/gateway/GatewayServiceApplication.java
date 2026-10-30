package com.adria.bank.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;

@SpringBootApplication
@EnableDiscoveryClient
public class GatewayServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayServiceApplication.class, args);
    }

    @Bean
    public RouteLocator customRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("account-route", r -> r.path("/api/accounts/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://account-service"))
                .route("transaction-route", r -> r.path("/api/transactions/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://transaction-service"))
                .route("chat-route", r -> r.path("/api/chat/**")
                        .filters(f -> f.stripPrefix(2))
                        .uri("lb://chatbot-service"))
                .build();
    }
}
