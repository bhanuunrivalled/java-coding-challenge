# Solution Overview

## Architecture

```
Client → CurrencyController → CurrencyQueryService → FxRateRepository → H2 (file-based)
                                                                              ↑
         BatchController → FxLoadService → Spring Batch Jobs → BundesbankClient → Bundesbank API
                                                                    ↑
                                                          Circuit Breaker (Resilience4j)
```

## How It Works

### Data Loading
- **Full load** runs once on startup if the database is empty. Fetches all historical EUR-FX rates from the Bundesbank SDMX API.
- **Delta load** runs daily at 16:30 CET (when ECB publishes new rates). Computes the date range automatically from the last stored rate to today.
- **Manual trigger** via `POST /api/batch/delta` for testing and operations.
- **Guard logic** prevents delta from running on an empty database and skips if data is already up to date.

### Storage
- H2 file-based database (`./data/fxrates-prod`) as required by the challenge.
- `AUTO_SERVER=TRUE` enables multi-instance access for horizontal scaling scenarios.
- Unique constraint on `(currency, rateDate)` prevents duplicate rate entries — safe for re-runs and overlapping delta loads.

### Resilience
- **Circuit breaker** on the Bundesbank API client (Resilience4j). Opens after 50% failure rate in a 10-call window, stays open 60s, then allows 3 test calls.
- **Timeouts**: 5s connect, 30s read — prevents thread pool exhaustion from hanging connections.
- **Graceful startup**: if the API is down on startup, the app starts without data and logs a warning. A manual full load endpoint could be added to retry without restarting the service.

### API Documentation

Swagger UI is available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) after starting the application. All endpoints are documented with descriptions, parameter examples, and response schemas.

### API Endpoints
| Endpoint | Description |
|---|---|
| `GET /api/currencies` | List all available currencies (cached) |
| `GET /api/rates/{currency}?date=` | Get rate at date (falls back to latest available) |
| `GET /api/rates/{currency}/history?page=&size=` | Paginated rate history |
| `GET /api/rates/convert?currency=&amount=&date=` | Convert amount to EUR |
| `POST /api/batch/delta` | Manually trigger delta load |
| `GET /actuator/health` | Health check with circuit breaker state |
| `GET /actuator/circuitbreakers` | Circuit breaker details |
| `GET /swagger-ui.html` | Interactive API documentation |

### Error Handling
- Global exception handler returns clean JSON errors (no stack traces).
- Consistent response format: `{ status, error, message, path, timestamp }`.

## Future Improvements

- **Metrics**: Add Micrometer counters/timers for API call rates, error rates, and batch job duration. Integrate with CloudWatch or Prometheus depending on deployment target.
- **Security**: Add rate limiting, API key authentication, and input validation hardening.
- **Database**: Replace H2 with a managed database for production. A relational DB (PostgreSQL/RDS) or even a NoSQL store (DynamoDB) would work — there are no relationships between entities, just key-value lookups by currency and date.
- **Deployment**: Separate batch jobs into an ECS scheduled task; API service scales independently behind ALB.
- **Observability**: Add request IDs for tracing requests across logs. Add a manual full load endpoint for operational recovery.

Looking forward to discussing these in more detail.
