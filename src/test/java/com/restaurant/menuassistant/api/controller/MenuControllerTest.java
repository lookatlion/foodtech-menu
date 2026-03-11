package com.restaurant.menuassistant.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.menuassistant.api.dto.MenuAnswerResponse;
import com.restaurant.menuassistant.api.dto.MenuQuestionRequest;
import com.restaurant.menuassistant.exception.EmbeddingServiceException;
import com.restaurant.menuassistant.exception.LlmServiceException;
import com.restaurant.menuassistant.rag.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MenuController.class)
class MenuControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean  private RagService ragService;

    @Test
    void shouldReturn200WithAnswerAndRelevantItems() throws Exception {
        when(ragService.ask(anyString())).thenReturn(
                new MenuAnswerResponse(
                        "I recommend the Veggie Burger!",
                        List.of("Veggie Burger", "Falafel Wrap")
                )
        );

        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new MenuQuestionRequest("What vegetarian options do you have?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("I recommend the Veggie Burger!"))
                .andExpect(jsonPath("$.relevantItems[0]").value("Veggie Burger"))
                .andExpect(jsonPath("$.relevantItems[1]").value("Falafel Wrap"));
    }

    @Test
    void shouldReturn400WhenQuestionIsBlank() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuQuestionRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Question must not be blank"));
    }

    @Test
    void shouldReturn400WhenQuestionIsNull() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void shouldReturn400WhenBodyIsMissing() throws Exception {
        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn503WhenLlmServiceFails() throws Exception {
        when(ragService.ask(anyString()))
                .thenThrow(new LlmServiceException("LLM error", new RuntimeException()));

        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuQuestionRequest("any question"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Unable to generate recommendation"));
    }

    @Test
    void shouldReturn503WhenEmbeddingServiceFails() throws Exception {
        when(ragService.ask(anyString()))
                .thenThrow(new EmbeddingServiceException("Embedding error", new RuntimeException()));

        mockMvc.perform(post("/api/menu/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MenuQuestionRequest("any question"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error").value("Embedding service unavailable"));
    }
}
