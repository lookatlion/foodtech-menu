package com.restaurant.menuassistant.rag.embedding.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record EmbeddingResponse(
        @JsonProperty("data") List<EmbeddingData> data
) {}
