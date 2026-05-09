# FX API Runtime Flow (Cache and Sync)

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant API as FX API
    participant Query as Query Service
    participant Store as Historical Store
    participant Sync as Background Sync Worker
    participant BB as Bundesbank

    Client->>API: GET /api/currencies
    API->>Query: getAvailableCurrencies()
    Query->>Store: read current snapshot

    alt Local snapshot is complete for requested data
        Store-->>Query: currencies snapshot
        Query-->>API: response payload
        API-->>Client: 200 OK
    else Missing data or latest-date lag
        Store-->>Query: empty result / lagging latest date
        Query-->>API: serve last known snapshot or empty list
        API-->>Client: 200 OK (degraded if needed)
        Query->>Sync: trigger/flag refresh
        Sync->>BB: fetch latest rates
        alt Sync success
            BB-->>Sync: latest rates
            Sync->>Store: upsert currencies and rates
        else Sync failure
            BB--xSync: timeout / unavailable
            Sync->>Sync: retry with backoff and lock protection
        end
    end
```

## Notes
- Client reads stay fast and independent from upstream availability.
- Sync runs in the background with controlled retries and locking.
- Historical rows are immutable by date; sync closes missing dates and latest-date lag.