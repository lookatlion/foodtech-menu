package com.restaurant.menuassistant.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.menuassistant.api.dto.MenuQuestionRequest;
import com.restaurant.menuassistant.rag.embedding.EmbeddingService;
import com.restaurant.menuassistant.rag.llm.LlmService;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
import com.restaurant.menuassistant.rag.vectorstore.VectorStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration test for the full HTTP → RAG pipeline.
 *
 * Uses @ActiveProfiles("test") which sets menu.auto-load=false, preventing
 * MenuDataLoader from calling the real OpenAI API during application startup.
 *
 * EmbeddingService and LlmService are mocked. VectorStore is the real in-memory
 * implementation, pre-populated in @BeforeEach, verifying the retrieval
 * and response-building path end-to-end.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuAssistantIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private VectorStore vectorStore;

    @MockBean private EmbeddingService embeddingService;
    @MockBean private LlmService llmService;

    @BeforeEach
    void setUp() {
        // Use uniform embeddings so all items appear equally relevant;
        // the important thing here is the HTTP → service → store → response path.
        float[] dummyEmbedding = {1.0f, 0.0f, 0.0f};

        when(embeddingService.generateEmbedding(anyString())).thenReturn(dummyEmbedding);
        when(llmService.generateAnswer(anyString(), any()))
                .thenReturn("I recommend the Veggie Burger for vegetarians!");

        vectorStore.add(new MenuItem("Veggie Burger", "Plant-based burger with lettuce, tomato and vegan sauce."), dummyEmbedding);
        vectorStore.add(new MenuItem("Falafel Wrap", "Falafel balls with tahini sauce, lettuce and tomato wrapped in pita."), dummyEmbedding);
        vectorStore.add(new MenuItem("Greek Salad", "Fresh tomatoes, cucumber, olives and feta cheese."), dummyEmbedding);
    }

    @AfterEach
    void tearDown() {
        vectorStore.clear();
    }

    @Test
    void shouldReturnAnswerAndRelevantItemsForValidQuestion() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MenuQuestionRequest("I'm vegetarian, what do you recommend?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("I recommend the Veggie Burger for vegetarians!"))
                .andExpect(jsonPath("$.relevantItems").isArray())
                .andExpect(jsonPath("$.relevantItems.length()").value(3));
    }

    @Test
    void shouldReturn400ForBlankQuestion() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuQuestionRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400ForMissingQuestion() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": null}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnAtMostTopKItems() throws Exception {
        // 3 items in store, top-k = 3 from config
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuQuestionRequest("What do you have?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.relevantItems.length()").value(
                        org.hamcrest.Matchers.lessThanOrEqualTo(3)));
    }
}
