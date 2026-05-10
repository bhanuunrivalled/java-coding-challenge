package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

/**
 * HTTP adapter for the Bundesbank SDMX REST API.
 * Fetches EUR exchange rate series as CSV — full history or a single day.
 * See:
 * https://www.bundesbank.de/en/statistics/time-series-databases/help-for-sdmx-web-service
 */
@Service
public class BundesbankClient {

    private static final String FLOW_REF = "BBEX3";
    private static final String SERIES_KEY = "D..EUR.BB.AC.000";
    private static final String CSV_ACCEPT = "text/csv";
    private static final String DATA_PATH = "/data/{flowRef}/{key}";

    private final RestClient restClient;

    public BundesbankClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.statistiken.bundesbank.de/rest")
                .build();
    }

    /** Fetches all historical EUR-FX rates — used for the one-time full load. */
    public String fetchFullLoadCsv() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DATA_PATH)
                        .queryParam("detail", "full")
                        .build(FLOW_REF, SERIES_KEY))
                .header(HttpHeaders.ACCEPT, CSV_ACCEPT)
                .retrieve()
                .body(String.class);
    }

    /** Fetches EUR-FX rates for a single date — used for the daily delta load. */
    public String fetchDeltaCsv(LocalDate date) {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(DATA_PATH)
                        .queryParam("startPeriod", date.toString())
                        .queryParam("endPeriod", date.toString())
                        .queryParam("detail", "dataonly")
                        .build(FLOW_REF, SERIES_KEY))
                .header(HttpHeaders.ACCEPT, CSV_ACCEPT)
                .retrieve()
                .body(String.class);
    }
}
