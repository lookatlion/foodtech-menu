package com.restaurant.menuassistant.rag;

import com.restaurant.menuassistant.api.dto.MenuAnswerResponse;
import com.restaurant.menuassistant.config.OpenAiProperties;
import com.restaurant.menuassistant.exception.VectorStoreException;
import com.restaurant.menuassistant.rag.embedding.EmbeddingService;
import com.restaurant.menuassistant.rag.llm.LlmService;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
import com.restaurant.menuassistant.rag.vectorstore.VectorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the full RAG pipeline:
 *
 *   1. Embed the user question
 *   2. Retrieve the top-K most semantically similar menu items
 *   3. Send retrieved items + question to the LLM for answer generation
 *   4. Return structured response
 */
@Service
public class RagService {

    private static final Logger log = LoggerFactory.getLogger(RagService.class);

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;
    private final LlmService llmService;
    private final OpenAiProperties properties;

    public RagService(
            EmbeddingService embeddingService,
            VectorStore vectorStore,
            LlmService llmService,
            OpenAiProperties properties
    ) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
        this.llmService = llmService;
        this.properties = properties;
    }

    public MenuAnswerResponse ask(String question) {
        log.info("RAG pipeline started for question: '{}'", question);

        // Step 1: Embed the question
        float[] queryEmbedding = embeddingService.generateEmbedding(question);

        // Step 2: Retrieve top-K similar menu items (assessment requires at least 1, max 3)
        List<MenuItem> relevantItems = vectorStore.findTopK(queryEmbedding, properties.topK());
        log.info("Retrieved {} items: {}", relevantItems.size(),
                relevantItems.stream().map(MenuItem::name).toList());

        if (relevantItems.isEmpty()) {
            throw new VectorStoreException("No relevant menu items found for the given question", null);
        }

        // Step 3: Generate answer
        String answer = llmService.generateAnswer(question, relevantItems);

        List<String> itemNames = relevantItems.stream().map(MenuItem::name).toList();
        return new MenuAnswerResponse(answer, itemNames);
    }
}
