package com.restaurant.menuassistant.rag.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingData(
        @JsonProperty("embedding") List<Float> embedding
) {}
