# FX API High-Level Architecture

```mermaid
flowchart LR
    Client[Clients\nWeb / Mobile / Partner Systems]

    subgraph FX[Read-Focused FX API Service]
        API[REST API Layer\nGET endpoints]
        Query[Query Service\nRead from historical store]
        Sync[Background Sync Worker\nScheduled + Controlled]
        Guard[Sync Controls\nLocking, retries, backoff]
    end

    Store[(Shared Historical FX Store\nCurrencies + Rates + Dates)]
    Bundesbank[(Bundesbank Data Source)]

    Client --> API
    API --> Query
    Query --> Store

    Sync --> Guard
    Guard --> Bundesbank
    Guard --> Store

    Store -. serves historical data .-> Query
```

## Notes
- Request path is read-optimized: API queries the shared historical store.
- Synchronization is decoupled from client requests and runs in background.
- Sync controls protect the upstream provider and keep data ingestion stable.