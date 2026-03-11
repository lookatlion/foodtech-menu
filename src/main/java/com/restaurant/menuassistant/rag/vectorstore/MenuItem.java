package com.restaurant.menuassistant.rag.vectorstore;

/**
 * Immutable domain object representing a single item on the restaurant menu.
 * toEmbeddingText() defines the canonical string sent to the embeddings API,
 * combining name and description for richer semantic representation.
 */
public record MenuItem(String name, String description) {

    public String toEmbeddingText() {
        return name + " - " + description;
    }
}
