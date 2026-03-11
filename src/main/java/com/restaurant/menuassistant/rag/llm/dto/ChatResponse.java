package com.restaurant.menuassistant.rag.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatResponse(
        @JsonProperty("choices") List<ChatChoice> choices
) {}
