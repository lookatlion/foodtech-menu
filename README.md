# Menu Assistant

A Java REST API that answers natural language questions about a restaurant menu using Retrieval Augmented Generation (RAG).

## Prerequisites

**With Docker (recommended — no Java or Gradle required):**
- Docker + Docker Compose
- An [OpenAI API key](https://platform.openai.com/api-keys)

**Without Docker:**
- Java 21+
- Gradle 8+ (or use the included `gradlew` wrapper)
- An [OpenAI API key](https://platform.openai.com/api-keys)

## Setup

```bash
git clone https://github.com/lookatlion/foodtech-menu.git
cd foodtech-menu
```

## Configure OpenAI API Key

Copy the example environment file and add your key:

```bash
cp .env.example .env
```

Open `.env` and replace the placeholder with your actual key:

```
OPENAI_API_KEY=sk-your-real-key-here
```

> The `.env` file is git-ignored and will never be committed.

## Run

**With Docker (any OS):**

```bash
docker compose up --build
```

**Without Docker — macOS / Linux:**

```bash
./gradlew bootRun
```

**Without Docker — Windows:**

```powershell
.\gradlew.bat bootRun
```

The server starts on **http://localhost:8080**. On first startup it embeds the menu items via the OpenAI API — this takes a few seconds.

## Run Tests

```bash
./gradlew test
```

Tests do not call the real OpenAI API.

## Example Requests

Once the server is running, copy-paste any of the commands below.

**Ask for a vegetarian recommendation:**

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "I am vegetarian and very hungry, what do you recommend?"}'
```

**Ask about spicy dishes:**

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What spicy dishes do you have?"}'
```

**Ask about drinks:**

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "What drinks are available?"}'
```

**Ask about desserts:**

```bash
curl -s -X POST http://localhost:8080/api/menu/ask \
  -H "Content-Type: application/json" \
  -d '{"question": "Do you have any desserts or sweet options?"}'
```

### Sample Response

```json
{
  "answer": "I recommend trying our Veggie Burger, which features a delicious plant-based patty topped with lettuce, tomato, and vegan sauce. The Falafel Wrap is also a great choice, filled with flavorful falafel balls, tahini sauce, lettuce, and tomato. Both options are filling and vegetarian-friendly!",
  "relevantItems": [
    "Veggie Burger",
    "Falafel Wrap",
    "Greek Salad"
  ]
}
