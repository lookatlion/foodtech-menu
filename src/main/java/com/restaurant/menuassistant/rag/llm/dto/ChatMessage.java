package com.restaurant.menuassistant.rag.llm.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record ChatMessage(
        @JsonProperty("role") String role,
        @JsonProperty("content") String content
) {

    public static ChatMessage system(String content) {
        return new ChatMessage("system", content);
    }

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }
}
