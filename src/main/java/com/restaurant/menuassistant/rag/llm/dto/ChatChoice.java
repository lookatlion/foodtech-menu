package com.restaurant.menuassistant.rag.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatChoice(
        @JsonProperty("message") ChatMessage message
) {}
