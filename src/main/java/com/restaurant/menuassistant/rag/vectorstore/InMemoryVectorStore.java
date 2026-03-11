package com.restaurant.menuassistant.rag.vectorstore;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * In-memory vector store backed by cosine similarity search.
 * Sufficient for the static 11-item dataset in this assessment.
 *
 * Thread safety: add() and clear() are synchronized. findTopK() operates on
 * an immutable snapshot to avoid holding a lock during the sort.
 *
 * Production note: for large or frequently-updated datasets, replace with
 * pgvector or a dedicated vector database without changing the VectorStore interface.
 */
@Component
public class InMemoryVectorStore implements VectorStore {

    private final List<EmbeddedItem> store = new ArrayList<>();

    @Override
    public synchronized void add(MenuItem item, float[] embedding) {
        store.add(new EmbeddedItem(item, embedding));
    }

    @Override
    public List<MenuItem> findTopK(float[] queryEmbedding, int k) {
        List<EmbeddedItem> snapshot = List.copyOf(store);
        return snapshot.stream()
                .sorted(Comparator.comparingDouble(item -> -cosineSimilarity(queryEmbedding, item.embedding())))
                .limit(k)
                .map(EmbeddedItem::menuItem)
                .toList();
    }

    @Override
    public synchronized void clear() {
        store.clear();
    }

    /**
     * Computes cosine similarity between two vectors.
     * Returns 0.0 for zero-magnitude vectors to avoid division by zero.
     */
    double cosineSimilarity(float[] a, float[] b) {
        double dot = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            normA += (double) a[i] * a[i];
            normB += (double) b[i] * b[i];
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
