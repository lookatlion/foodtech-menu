package com.restaurant.menuassistant.rag.vectorstore;

import java.util.List;

/**
 * Port interface for the vector store.
 * Decouples the RAG pipeline from the concrete storage implementation,
 * making it straightforward to swap in pgvector or Chroma in production.
 */
public interface VectorStore {

    void add(MenuItem item, float[] embedding);

    List<MenuItem> findTopK(float[] queryEmbedding, int k);

    void clear();
}
