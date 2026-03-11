package com.restaurant.menuassistant.rag.embedding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.exception.EmbeddingServiceException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmbeddingServiceTest {

    private MockWebServer mockWebServer;
    private EmbeddingService embeddingService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        String baseUrl = mockWebServer.url("/").toString();

        OpenAiProperties properties = new OpenAiProperties(
                "test-key", baseUrl, "gpt-4o", "text-embedding-3-small", 3, 0, 100
        );

        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer test-key")
                .build();

        embeddingService = new EmbeddingService(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnEmbeddingVectorOnSuccess() throws Exception {
        List<Float> expectedEmbedding = List.of(0.1f, 0.2f, 0.3f);
        Map<String, Object> body = Map.of(
                "data", List.of(Map.of("embedding", expectedEmbedding))
        );
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));

        float[] result = embeddingService.generateEmbedding("Veggie Burger - Plant-based burger");

        assertThat(result).containsExactly(0.1f, 0.2f, 0.3f);
    }

    @Test
    void shouldSendCorrectModelAndInputToApi() throws Exception {
        Map<String, Object> body = Map.of(
                "data", List.of(Map.of("embedding", List.of(0.1f)))
        );
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));

        embeddingService.generateEmbedding("test input");

        RecordedRequest request = mockWebServer.takeRequest();
        String requestBody = request.getBody().readUtf8();
        assertThat(requestBody).contains("\"model\":\"text-embedding-3-small\"");
        assertThat(requestBody).contains("\"input\":\"test input\"");
    }

    @Test
    void shouldThrowEmbeddingServiceExceptionOnApiError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("test"))
                .isInstanceOf(EmbeddingServiceException.class)
                .hasMessageContaining("Failed to generate embedding");
    }

    @Test
    void shouldThrowEmbeddingServiceExceptionOnUnauthorized() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(401).setBody("{\"error\":\"invalid api key\"}"));

        assertThatThrownBy(() -> embeddingService.generateEmbedding("test"))
                .isInstanceOf(EmbeddingServiceException.class);
    }
}
