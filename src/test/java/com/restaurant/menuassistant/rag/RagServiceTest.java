package com.restaurant.menuassistant.rag;

import com.restaurant.menuassistant.api.dto.MenuAnswerResponse;
import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.rag.embedding.EmbeddingService;
import com.restaurant.menuassistant.rag.llm.LlmService;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
import com.restaurant.menuassistant.rag.vectorstore.VectorStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock private EmbeddingService embeddingService;
    @Mock private VectorStore vectorStore;
    @Mock private LlmService llmService;
    @Mock private OpenAiProperties properties;

    @InjectMocks
    private RagService ragService;

    @Test
    void shouldOrchestrateFullRagPipelineAndReturnResponse() {
        float[] embedding = {0.1f, 0.2f, 0.3f};
        List<MenuItem> items = List.of(
                new MenuItem("Veggie Burger", "Plant-based burger"),
                new MenuItem("Falafel Wrap", "Falafel with pita")
        );

        when(properties.topK()).thenReturn(3);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(embedding);
        when(vectorStore.findTopK(any(float[].class), anyInt())).thenReturn(items);
        when(llmService.generateAnswer(anyString(), any())).thenReturn("I recommend the Veggie Burger!");

        MenuAnswerResponse response = ragService.ask("I'm vegetarian, what do you recommend?");

        assertThat(response.answer()).isEqualTo("I recommend the Veggie Burger!");
        assertThat(response.relevantItems()).containsExactly("Veggie Burger", "Falafel Wrap");
    }

    @Test
    void shouldPassTopKFromPropertiesToVectorStore() {
        List<MenuItem> items = List.of(new MenuItem("Fries", "Crispy golden potato fries."));
        when(properties.topK()).thenReturn(3);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(vectorStore.findTopK(any(), anyInt())).thenReturn(items);
        when(llmService.generateAnswer(anyString(), any())).thenReturn("Try the fries!");

        ragService.ask("any question");

        verify(vectorStore).findTopK(any(), eq(3));
    }

    @Test
    void shouldPassQuestionEmbeddingToVectorStore() {
        float[] questionEmbedding = {0.5f, 0.6f};
        List<MenuItem> items = List.of(new MenuItem("Falafel Wrap", "Falafel with pita"));
        when(properties.topK()).thenReturn(3);
        when(embeddingService.generateEmbedding("spicy food?")).thenReturn(questionEmbedding);
        when(vectorStore.findTopK(any(), anyInt())).thenReturn(items);
        when(llmService.generateAnswer(anyString(), any())).thenReturn("Answer");

        ragService.ask("spicy food?");

        verify(vectorStore).findTopK(questionEmbedding, 3);
    }

    @Test
    void shouldThrowVectorStoreExceptionWhenNoItemsRetrieved() {
        when(properties.topK()).thenReturn(3);
        when(embeddingService.generateEmbedding(anyString())).thenReturn(new float[]{0.1f});
        when(vectorStore.findTopK(any(), anyInt())).thenReturn(List.of());

        org.junit.jupiter.api.Assertions.assertThrows(
                com.restaurant.menuassistant.exception.VectorStoreException.class,
                () -> ragService.ask("any question")
        );
    }

    // Required for verify with primitive int — static import helper
    private static int eq(int value) {
        return org.mockito.ArgumentMatchers.eq(value);
    }
}
