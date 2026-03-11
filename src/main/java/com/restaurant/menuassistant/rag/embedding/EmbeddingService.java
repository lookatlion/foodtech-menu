package com.restaurant.menuassistant.rag.embedding;

import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.exception.EmbeddingServiceException;
import com.restaurant.menuassistant.rag.embedding.dto.EmbeddingRequest;
import com.restaurant.menuassistant.rag.embedding.dto.EmbeddingResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;

@Service
public class EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingService.class);

    private final WebClient webClient;
    private final OpenAiProperties properties;

    public EmbeddingService(WebClient openAiWebClient, OpenAiProperties properties) {
        this.webClient = openAiWebClient;
        this.properties = properties;
    }

    /**
     * Calls the OpenAI embeddings endpoint and returns the embedding vector.
     *
     * @throws EmbeddingServiceException if the API call fails for any reason
     */
    public float[] generateEmbedding(String text) {
        log.debug("Requesting embedding for text of length {}", text.length());
        try {
            var mono = webClient.post()
                    .uri("/embeddings")
                    .bodyValue(new EmbeddingRequest(text, properties.embeddingModel()))
                    .retrieve()
                    .bodyToMono(EmbeddingResponse.class);

            if (properties.retryMaxAttempts() > 0) {
                mono = mono.retryWhen(Retry.backoff(properties.retryMaxAttempts(),
                                Duration.ofMillis(properties.retryMinBackoffMs()))
                        .filter(throwable -> throwable instanceof WebClientResponseException ex
                                && ex.getStatusCode().is5xxServerError())
                        .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
            }

            EmbeddingResponse response = mono.block();

            List<Float> embedding = response.data().get(0).embedding();
            float[] result = new float[embedding.size()];
            for (int i = 0; i < embedding.size(); i++) {
                result[i] = embedding.get(i);
            }

            log.debug("Received embedding vector of dimension {}", result.length);
            return result;

        } catch (WebClientResponseException e) {
            log.error("OpenAI embeddings API returned HTTP {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingServiceException("Failed to generate embedding", e);
        } catch (Exception e) {
            log.error("Unexpected error generating embedding", e);
            throw new EmbeddingServiceException("Failed to generate embedding", e);
        }
    }
}
