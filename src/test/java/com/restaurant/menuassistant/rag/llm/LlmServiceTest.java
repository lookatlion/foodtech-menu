package com.restaurant.menuassistant.rag.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.exception.LlmServiceException;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
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

class LlmServiceTest {

    private MockWebServer mockWebServer;
    private LlmService llmService;
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

        llmService = new LlmService(webClient, properties);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnGeneratedAnswerFromLlm() throws Exception {
        enqueueChatResponse("Try the Veggie Burger!");

        List<MenuItem> items = List.of(new MenuItem("Veggie Burger", "Plant-based burger"));
        String answer = llmService.generateAnswer("vegetarian options?", items);

        assertThat(answer).isEqualTo("Try the Veggie Burger!");
    }

    @Test
    void shouldIncludeAllMenuItemsInRequestBody() throws Exception {
        enqueueChatResponse("Here is my recommendation.");

        List<MenuItem> items = List.of(
                new MenuItem("Veggie Burger", "Plant-based burger"),
                new MenuItem("Falafel Wrap", "Falafel with pita")
        );
        llmService.generateAnswer("I'm vegetarian", items);

        RecordedRequest request = mockWebServer.takeRequest();
        String body = request.getBody().readUtf8();
        assertThat(body).contains("Veggie Burger");
        assertThat(body).contains("Falafel Wrap");
        assertThat(body).contains("\"model\":\"gpt-4o\"");
    }

    @Test
    void shouldThrowLlmServiceExceptionOnApiError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> llmService.generateAnswer("question?",
                List.of(new MenuItem("Item", "Desc"))))
                .isInstanceOf(LlmServiceException.class)
                .hasMessageContaining("Failed to generate recommendation");
    }

    @Test
    void shouldThrowLlmServiceExceptionOnRateLimitError() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(429).setBody("{\"error\":\"rate limit exceeded\"}"));

        assertThatThrownBy(() -> llmService.generateAnswer("question?",
                List.of(new MenuItem("Item", "Desc"))))
                .isInstanceOf(LlmServiceException.class);
    }

    private void enqueueChatResponse(String content) throws Exception {
        Map<String, Object> body = Map.of(
                "choices", List.of(
                        Map.of("message", Map.of("role", "assistant", "content", content))
                )
        );
        mockWebServer.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(body))
                .addHeader("Content-Type", "application/json"));
    }
}
