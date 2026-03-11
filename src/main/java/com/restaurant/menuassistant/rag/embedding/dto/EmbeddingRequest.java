package com.restaurant.menuassistant.rag.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EmbeddingRequest(
        @JsonProperty("input") String input,
        @JsonProperty("model") String model
) {}
