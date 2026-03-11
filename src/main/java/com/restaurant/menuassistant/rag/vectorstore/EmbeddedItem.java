package com.restaurant.menuassistant.rag.vectorstore;

/**
 * Associates a MenuItem with its pre-computed embedding vector.
 * Stored in the in-memory vector index at startup.
 */
public record EmbeddedItem(MenuItem menuItem, float[] embedding) {}
