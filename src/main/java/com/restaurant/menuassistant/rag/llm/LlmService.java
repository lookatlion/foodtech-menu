package com.restaurant.menuassistant.rag.llm;

import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.exception.LlmServiceException;
import com.restaurant.menuassistant.rag.llm.dto.ChatMessage;
import com.restaurant.menuassistant.rag.llm.dto.ChatRequest;
import com.restaurant.menuassistant.rag.llm.dto.ChatResponse;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private static final String SYSTEM_PROMPT = """
            You are a helpful restaurant assistant.
            Use the provided menu items to answer the customer's question.
            Be concise, friendly, and recommend specific items by name.
            """;

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public LlmService(WebClient openAiWebClient, OpenAiProperties properties) {
        this.webClient = openAiWebClient;
        this.properties = properties;
    }

    /**
     * Sends the retrieved menu items and user question to the LLM and returns the generated answer.
     *
     * @throws LlmServiceException if the chat completions API call fails
     */
    public String generateAnswer(String question, List<MenuItem> menuItems) {
        log.debug("Generating answer for question '{}' using {} menu items", question, menuItems.size());

        String menuContext = menuItems.stream()
                .map(item -> "- " + item.name() + ": " + item.description())
                .collect(Collectors.joining("\n"));

        String userPrompt = """
                Menu items:
                %s

                Question:
                %s

                Answer the question and recommend relevant items.
                """.formatted(menuContext, question);

        try {
            var mono = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(new ChatRequest(
                            properties.model(),
                            List.of(
                                    ChatMessage.system(SYSTEM_PROMPT),
                                    ChatMessage.user(userPrompt)
                            )
                    ))
                    .retrieve()
                    .bodyToMono(ChatResponse.class);

            if (properties.retryMaxAttempts() > 0) {
                mono = mono.retryWhen(Retry.backoff(properties.retryMaxAttempts(),
                                Duration.ofMillis(properties.retryMinBackoffMs()))
                        .filter(throwable -> throwable instanceof WebClientResponseException ex
                                && ex.getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }

            ChatResponse response = mono.block();

            String answer = response.choices().get(0).message().content();
            log.debug("LLM answer generated successfully");
            return answer;

        } catch (WebClientResponseException e) {
            log.error("OpenAI chat API returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new LlmServiceException("Failed to generate recommendation", e);
        } catch (Exception e) {
            log.error("Unexpected error generating LLM answer", e);
            throw new LlmServiceException("Failed to generate recommendation", e);
        }
    }
}
