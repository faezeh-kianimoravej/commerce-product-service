package com.faezeh.commerce.product;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@OpenAPIDefinition(
        info = @Info(
                title = "Commerce Product Service API",
                version = "v1",
                description = "REST API for managing products in the Commerce microservices platform."
        )
)
@SpringBootApplication
public class CommerceProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceProductServiceApplication.class, args);
    }
}
