# eBank — Microservices & AI Chatbot

Banking demo made of Spring Boot microservices (customers, bank accounts) behind a Spring Cloud Gateway with Eureka discovery.
A Spring AI agent reads and creates live data through MCP tools, and is reachable from a REST API, an Angular client, Telegram and Discord.

**Stack:** Java 21, Spring Boot 3.5.10, Spring Cloud (Eureka, Gateway, OpenFeign, Resilience4j), Spring AI 1.1.2 (OpenAI, MCP over Streamable HTTP), H2, Angular 21 + Bootstrap 5, Telegram long polling, Discord (JDA).

| Service | Port | Role |
|---|---|---|
| `customer-service` | 8056 | Customers (REST + MCP `/mcp`) |
| `ebank-service` | 8057 | Accounts, Feign + circuit breaker (REST + MCP `/mcp`) |
| `ebank-bot` | 8058 | AI agent: REST, Telegram, Discord |
| `discovery-service` | 8761 | Eureka server |
| `gateway-service` | 9999 | Reactive gateway, dynamic routes |
| `frontend/angular-front` | 4200 | Angular client |

## Run

1. Copy `.env.example` to `.env` (never committed) and fill `OPENAI_API_KEY`, `OPENAI_MODEL`, `TELEGRAM_BOT_TOKEN`, `DISCORD_BOT_TOKEN`. `ebank-bot` loads it automatically; a missing Telegram/Discord token only disables that bot.
2. Build: `cd backend && mvn package` (JDK 21).
3. Start in this order (`java -jar backend/<service>/target/<service>-0.0.1-SNAPSHOT.jar`): **discovery-service**, then **customer-service** and **ebank-service**, then **gateway-service** and **ebank-bot** (the bot needs both MCP servers up).
4. Frontend: `cd frontend/angular-front && npm install && npm start`, open http://localhost:4200.

## Main URLs

- Gateway (`/{service-id}/{path}`): `http://localhost:9999/customer-service/customers`, `.../ebank-service/accounts`, `.../ebank-bot/chat?query=...`, `.../ebank-bot/chat/stream?query=...`
- Eureka: http://localhost:8761 · Swagger: `http://localhost:805{6,7,8}/swagger-ui/index.html` · MCP: `http://localhost:{8056,8057}/mcp`
- Postman collection: [`postman/ebank-api.postman_collection.json`](postman/ebank-api.postman_collection.json)

## Design notes

- Accounts keep only a `customerId`; customer data is fetched with OpenFeign and attached to a transient field. Account creation checks that the customer exists and fails safely (400 unknown customer, 503 if customer-service is down).
- Circuit breaker `customerService` (reads only): opens at ≥50% failures over 5 calls (min 3), stays open 10 s, then 2 trial calls. While open, accounts still return with customer `Not available`.
- `/chat/stream` sends Server-Sent Events; Angular reads the download-progress events and shows the answer as it grows.

## Checklist

- [x] Customers and accounts REST, seeded H2 data (3 customers, 5 accounts each)
- [x] Eureka registration, static then dynamic gateway routes, CORS for Angular
- [x] Feign enrichment, customer validation, circuit-breaker fallback and recovery
- [x] MCP servers; agent discovers all 7 tools at startup
- [x] Chat memory, `/chat` and `/chat/stream` (streaming checked through the gateway with a stub model)
- [x] Angular: customers, accounts, loading spinner + interceptor, streamed chat
- [ ] Chatbot tested end to end with the real OpenAI model
- [ ] Telegram bot tested with a real token
- [ ] Discord bot tested with a real token

## Telegram
![Telegram — customer query](docs/screenshots/telegram-customers.png)
![Telegram — bank account query](docs/screenshots/telegram-accounts.png)

## Discord
![Discord — customer query](docs/screenshots/discord-customers.png)
![Discord — bank account query](docs/screenshots/discord-accounts.png)
