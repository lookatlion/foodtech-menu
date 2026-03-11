package com.restaurant.menuassistant.rag.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatRequest(
        @JsonProperty("model") String model,
        @JsonProperty("messages") List<ChatMessage> messages
) {}
