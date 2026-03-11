package com.restaurant.menuassistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record MenuQuestionRequest(
        @NotBlank(message = "Question must not be blank")
        @Schema(description = "Natural language question about the menu", example = "What vegetarian options do you have?")
        String question
) {}
