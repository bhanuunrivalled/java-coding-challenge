package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

/**
 * HTTP adapter for the Bundesbank SDMX REST API.
 * Fetches EUR exchange rate series as CSV — full history or a date range.
 * Wrapped with a circuit breaker for resilience against API outages.
 *
 * TODO: Add Micrometer metrics (counters for API calls/errors, timer for
 * duration)
 * once observability strategy is finalized.
 */
@Service
public class BundesbankClient {

        private static final Logger LOGGER = LoggerFactory.getLogger(BundesbankClient.class);
        private static final String CIRCUIT_BREAKER_NAME = "bundesbankApi";
        private static final String FLOW_REF = "BBEX3";
        private static final String SERIES_KEY = "D..EUR.BB.AC.000";
        private static final String DATA_PATH = "/data/{flowRef}/{key}";

        private final RestClient restClient;

        public BundesbankClient(RestClient.Builder restClientBuilder,
                        @org.springframework.beans.factory.annotation.Value("${bundesbank.api.base-url}") String baseUrl) {
                this.restClient = restClientBuilder
                                .baseUrl(baseUrl)
                                .build();
        }

        /** Fetches all historical EUR-FX rates — used for the one-time full load. */
        @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFullLoad")
        public String fetchFullLoadCsv() {
                LOGGER.info("Fetching full load CSV from Bundesbank API");
                return restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(DATA_PATH)
                                                .queryParam("detail", "full")
                                                .queryParam("format", "sdmx_csv")
                                                .build(FLOW_REF, SERIES_KEY))
                                .retrieve()
                                .body(String.class);
        }

        /** Fetches EUR-FX rates for a date range — used for the daily delta load. */
        @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDeltaLoad")
        public String fetchDeltaCsv(LocalDate startDate, LocalDate endDate) {
                if (startDate.isAfter(endDate)) {
                        throw new IllegalArgumentException(
                                        "startDate %s must not be after endDate %s".formatted(startDate, endDate));
                }
                LOGGER.info("Fetching delta CSV from Bundesbank API: {} to {}", startDate, endDate);
                return restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(DATA_PATH)
                                                .queryParam("startPeriod", startDate.toString())
                                                .queryParam("endPeriod", endDate.toString())
                                                .queryParam("detail", "full")
                                                .queryParam("format", "sdmx_csv")
                                                .build(FLOW_REF, SERIES_KEY))
                                .retrieve()
                                .body(String.class);
        }

        private String fallbackFullLoad(Exception ex) {
                LOGGER.error("Bundesbank API unavailable for full load (circuit breaker open or error): {}",
                                ex.getMessage());
                throw new BundesbankApiException("Bundesbank API unavailable during full load", ex);
        }

        private String fallbackDeltaLoad(LocalDate startDate, LocalDate endDate, Exception ex) {
                LOGGER.error("Bundesbank API unavailable for delta load {} to {} (circuit breaker open or error): {}",
                                startDate, endDate, ex.getMessage());
                throw new BundesbankApiException(
                                "Bundesbank API unavailable during delta load for " + startDate + " to " + endDate, ex);
        }
}
