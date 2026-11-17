package com.luxeperfume.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI luxePerfumeOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("LUXE PERFUME API")
                        .version("v1")
                        .description("REST API for the LUXE PERFUME e-commerce platform.")
                        .contact(new Contact().name("LUXE PERFUME Engineering Team")));
    }
}
