package com.restaurant.menuassistant.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Type-safe, immutable configuration for the OpenAI API.
 * Values are bound from application.yml under the "openai" prefix.
 * The API key is resolved from the OPENAI_API_KEY environment variable.
 */
@Validated
@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(
        @NotBlank String apiKey,
        @NotBlank String baseUrl,
        @NotBlank String model,
        @NotBlank String embeddingModel,
        @Positive int topK,
        int retryMaxAttempts,
        long retryMinBackoffMs
) {}
