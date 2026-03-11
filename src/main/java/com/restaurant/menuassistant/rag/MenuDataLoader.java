package com.restaurant.menuassistant.rag;

import com.restaurant.menuassistant.rag.embedding.EmbeddingService;
import com.restaurant.menuassistant.rag.vectorstore.MenuItem;
import com.restaurant.menuassistant.rag.vectorstore.VectorStore;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Generates embeddings for every menu item and loads them into the vector store at startup.
 *
 * Disabled when menu.auto-load=false (used in tests to avoid real OpenAI calls during
 * application context initialization).
 *
 * Design note: startup loading is preferred over lazy/first-request loading because:
 *   - the dataset is small and static
 *   - the vector index is guaranteed warm before the first user request
 *   - behaviour is predictable and easy to observe in logs
 *
 * In production with a dynamic menu, this would be replaced with a background
 * re-indexing pipeline or an admin /index endpoint.
 */
@Component
@ConditionalOnProperty(name = "menu.auto-load", havingValue = "true", matchIfMissing = true)
public class MenuDataLoader {

    private static final Logger log = LoggerFactory.getLogger(MenuDataLoader.class);

    private static final List<MenuItem> MENU_ITEMS = List.of(
            new MenuItem("Cheeseburger", "Grilled beef burger with cheddar cheese, lettuce, tomato and house sauce."),
            new MenuItem("Chicken Burger", "Crispy fried chicken breast with lettuce and mayo."),
            new MenuItem("Margherita Pizza", "Classic pizza with tomato sauce, mozzarella and fresh basil."),
            new MenuItem("Pepperoni Pizza", "Pizza topped with pepperoni slices and mozzarella."),
            new MenuItem("Greek Salad", "Fresh tomatoes, cucumber, olives and feta cheese."),
            new MenuItem("Fries", "Crispy golden potato fries."),
            new MenuItem("Large Fries", "Large portion of crispy golden fries."),
            new MenuItem("Coke", "Classic Coca-Cola soft drink."),
            new MenuItem("Milkshake", "Creamy vanilla milkshake."),
            new MenuItem("Veggie Burger", "Plant-based burger with lettuce, tomato and vegan sauce."),
            new MenuItem("Falafel Wrap", "Falafel balls with tahini sauce, lettuce and tomato wrapped in pita.")
    );

    private final EmbeddingService embeddingService;
    private final VectorStore vectorStore;

    public MenuDataLoader(EmbeddingService embeddingService, VectorStore vectorStore) {
        this.embeddingService = embeddingService;
        this.vectorStore = vectorStore;
    }

    @PostConstruct
    public void loadMenuEmbeddings() {
        log.info("Indexing {} menu items into vector store...", MENU_ITEMS.size());
        MENU_ITEMS.forEach(item -> {
            float[] embedding = embeddingService.generateEmbedding(item.toEmbeddingText());
            vectorStore.add(item, embedding);
            log.debug("Indexed: {}", item.name());
        });
        log.info("Menu index ready.");
    }
}
