package com.restaurant.menuassistant.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI menuAssistantOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Menu Assistant API")
                        .description("AI-powered restaurant menu assistant using RAG (Embeddings + Vector Search)")
                        .version("1.0.0"));
    }
}
