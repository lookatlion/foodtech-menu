package com.restaurant.menuassistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ErrorResponse(
        @Schema(description = "Human-readable error message")
        String error
) {}
