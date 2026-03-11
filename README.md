# Menu Assistant — AI-powered RAG API

A Spring Boot REST API that answers natural language questions about a restaurant menu using **Retrieval Augmented Generation (RAG)**: embeddings, in-memory vector search, and LLM generation.

---

## Architecture

```
POST /api/menu/ask
       │
  MenuController          (validates input, delegates to service)
       │
  RagService              (orchestrates the pipeline)
  ├── EmbeddingService    (WebClient → OpenAI /v1/embeddings)
  ├── InMemoryVectorStore (cosine similarity, top-K retrieval)
  └── LlmService          (WebClient → OpenAI /v1/chat/completions)
```

### RAG Pipeline

1. **Startup** — `MenuDataLoader` embeds all 11 menu items via `@PostConstruct` and stores them in the vector index
2. **Query** — the user's question is embedded using the same model
3. **Retrieval** — top-3 most similar items are retrieved by cosine similarity
4. **Generation** — retrieved items + question are sent to the LLM, which returns a natural language recommendation

### Key Design Decisions

| Decision | Rationale |
|---|---|
| Spring WebClient over Spring AI | Keeps the RAG flow explicit and easy to review; no hidden abstractions |
| Layered architecture | Aligns with standard Spring Boot conventions; straightforward for any Java team |
| In-memory vector store | Dataset is small and static; avoids external dependencies |
| `@PostConstruct` indexing | Guarantees warm index before first request; predictable startup behaviour |
| `@ConditionalOnProperty` on `MenuDataLoader` | Cleanly prevents real API calls during test context initialization |
| Custom exceptions + `@RestControllerAdvice` | Keeps service layers framework-agnostic; consistent structured error responses |
| Java 21 records | Immutable DTOs and domain objects with zero boilerplate |

---

## Requirements

- Java 21+
- Gradle 8.x (or use `./gradlew`)
- An OpenAI API key

---

## Configuration

Credentials are loaded from a `.env` file in the project root. Copy the example and fill in your key:

```bash
cp .env.example .env
```

Then edit [.env](.env):

```
OPENAI_API_KEY=sk-your-real-key-here
```

The `.env` file is listed in `.gitignore` and is never committed. The `.env.example` file is committed as a template for other developers.

All other settings are in [src/main/resources/application.yml](src/main/resources/application.yml):

```yaml
openai:
  model: gpt-4o                        # LLM model
  embedding-model: text-embedding-3-small
  top-k: 3                             # number of items retrieved per query
```

---

## Running locally

```bash
# Clone and enter the project
cd menu-assistant

# Run (API key must be set in environment)
OPENAI_API_KEY=sk-... ./gradlew bootRun
```

The API starts on `http://localhost:8080`.

Swagger UI is available at: `http://localhost:8080/swagger-ui.html`

---

## Running tests

```bash
./gradlew test
```

Tests never call the real OpenAI API. `application-test.yml` sets `menu.auto-load=false` to prevent embedding generation during context startup, and MockWebServer is used for HTTP-level unit tests.

---

## Example requests

### Vegetarian recommendation

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "I am vegetarian and very hungry, what do you recommend?"}' | jq
```

### Spicy options

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What spicy dishes do you have?"}' | jq
```

### Drinks

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What drinks are available?"}' | jq
```

### Example response

```json
{
  "answer": "For a vegetarian looking for something filling, I'd recommend the Veggie Burger — a plant-based burger with lettuce, tomato and vegan sauce. The Falafel Wrap is also a great option: falafel balls with tahini, lettuce and tomato in a pita. Both are satisfying and entirely plant-based.",
  "relevantItems": [
    "Veggie Burger",
    "Falafel Wrap",
    "Greek Salad"
  ]
}
```

### Validation error (400)

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": ""}' | jq
```

```json
{
  "error": "Question must not be blank"
}
```

---
