package com.restaurant.menuassistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MenuAnswerResponse(
        @Schema(description = "AI-generated recommendation based on the question and retrieved menu items")
        String answer,

        @Schema(description = "Names of the top-3 most relevant menu items retrieved from the vector store")
        List<String> relevantItems
) {}
