package com.restaurant.menuassistant.rag.vectorstore;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class InMemoryVectorStoreTest {

    private InMemoryVectorStore vectorStore;

    @BeforeEach
    void setUp() {
        vectorStore = new InMemoryVectorStore();
    }

    @Test
    void shouldReturnTopKItemsRankedBySimilarity() {
        var burger = new MenuItem("Veggie Burger", "Plant-based burger");
        var pizza = new MenuItem("Pizza", "Tomato and cheese");
        var salad = new MenuItem("Salad", "Fresh green salad");

        // burger and salad both close to [1, 0]; pizza close to [0, 1]
        vectorStore.add(burger, new float[]{1.0f, 0.0f});
        vectorStore.add(pizza, new float[]{0.0f, 1.0f});
        vectorStore.add(salad, new float[]{0.9f, 0.1f});

        List<MenuItem> results = vectorStore.findTopK(new float[]{1.0f, 0.0f}, 2);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).name()).isEqualTo("Veggie Burger");
        assertThat(results.get(1).name()).isEqualTo("Salad");
    }

    @Test
    void shouldReturnAllItemsWhenKExceedsStoreSize() {
        vectorStore.add(new MenuItem("Item A", "Desc A"), new float[]{1.0f, 0.0f});
        vectorStore.add(new MenuItem("Item B", "Desc B"), new float[]{0.0f, 1.0f});

        List<MenuItem> results = vectorStore.findTopK(new float[]{1.0f, 0.0f}, 10);

        assertThat(results).hasSize(2);
    }

    @Test
    void shouldReturnEmptyListWhenStoreIsEmpty() {
        List<MenuItem> results = vectorStore.findTopK(new float[]{1.0f, 0.0f}, 3);

        assertThat(results).isEmpty();
    }

    @Test
    void shouldClearAllItemsFromStore() {
        vectorStore.add(new MenuItem("Item A", "Desc A"), new float[]{1.0f, 0.0f});
        vectorStore.clear();

        assertThat(vectorStore.findTopK(new float[]{1.0f, 0.0f}, 3)).isEmpty();
    }

    @Test
    void cosineSimilarity_identicalVectors_returnsOne() {
        double similarity = vectorStore.cosineSimilarity(
                new float[]{1.0f, 2.0f, 3.0f},
                new float[]{1.0f, 2.0f, 3.0f}
        );

        assertThat(similarity).isCloseTo(1.0, within(1e-6));
    }

    @Test
    void cosineSimilarity_orthogonalVectors_returnsZero() {
        double similarity = vectorStore.cosineSimilarity(
                new float[]{1.0f, 0.0f},
                new float[]{0.0f, 1.0f}
        );

        assertThat(similarity).isCloseTo(0.0, within(1e-6));
    }

    @Test
    void cosineSimilarity_zeroVector_returnsZero() {
        double similarity = vectorStore.cosineSimilarity(
                new float[]{0.0f, 0.0f},
                new float[]{1.0f, 0.0f}
        );

        assertThat(similarity).isEqualTo(0.0);
    }
}
