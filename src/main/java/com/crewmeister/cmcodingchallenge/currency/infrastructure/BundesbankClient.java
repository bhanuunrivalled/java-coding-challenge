package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import com.crewmeister.cmcodingchallenge.currency.batch.FxCsvRateRow;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Fetches EUR exchange rate data from the Bundesbank SDMX REST API.
 * Handles HTTP communication, CSV parsing, circuit breaker, and timeouts.
 */
@Service
public class BundesbankClient implements FxRateProvider {

        private static final Logger LOGGER = LoggerFactory.getLogger(BundesbankClient.class);
        private static final String CIRCUIT_BREAKER_NAME = "bundesbankApi";
        private static final String FLOW_REF = "BBEX3";
        private static final String SERIES_KEY = "D..EUR.BB.AC.000";
        private static final String DATA_PATH = "/data/{flowRef}/{key}";

        private final RestClient restClient;

        public BundesbankClient(RestClient.Builder restClientBuilder,
                        @Value("${bundesbank.api.base-url}") String baseUrl,
                        @Value("${bundesbank.api.connect-timeout}") Duration connectTimeout,
                        @Value("${bundesbank.api.read-timeout}") Duration readTimeout) {
                ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                                .withConnectTimeout(connectTimeout)
                                .withReadTimeout(readTimeout);
                this.restClient = restClientBuilder
                                .baseUrl(baseUrl)
                                .requestFactory(ClientHttpRequestFactories.get(settings))
                                .build();
        }

        @Override
        @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackFullLoad")
        public List<FxCsvRateRow> fetchFullLoad() {
                LOGGER.info("Fetching full load from Bundesbank API");
                String csv = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(DATA_PATH)
                                                .queryParam("detail", "full")
                                                .queryParam("format", "sdmx_csv")
                                                .build(FLOW_REF, SERIES_KEY))
                                .retrieve()
                                .body(String.class);
                return parseCsv(csv);
        }

        @Override
        @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "fallbackDelta")
        public List<FxCsvRateRow> fetchDelta(LocalDate startDate, LocalDate endDate) {
                if (startDate.isAfter(endDate)) {
                        throw new IllegalArgumentException(
                                        "startDate %s must not be after endDate %s".formatted(startDate, endDate));
                }
                LOGGER.info("Fetching delta from Bundesbank API: {} to {}", startDate, endDate);
                String csv = restClient.get()
                                .uri(uriBuilder -> uriBuilder
                                                .path(DATA_PATH)
                                                .queryParam("startPeriod", startDate.toString())
                                                .queryParam("endPeriod", endDate.toString())
                                                .queryParam("detail", "full")
                                                .queryParam("format", "sdmx_csv")
                                                .build(FLOW_REF, SERIES_KEY))
                                .retrieve()
                                .body(String.class);
                return parseCsv(csv);
        }

        /**
         * Parses Bundesbank SDMX CSV format into rate rows.
         * Columns:
         * DATAFLOW;BBK_STD_FREQ;BBK_STD_CURRENCY;...;TIME_PERIOD;OBS_VALUE;...;BBK_ID;...
         */
        private List<FxCsvRateRow> parseCsv(String csv) {
                if (csv == null || csv.isBlank()) {
                        return List.of();
                }
                String[] lines = csv.split("\n");
                List<FxCsvRateRow> rows = new ArrayList<>();
                // Skip header (line 0)
                for (int i = 1; i < lines.length; i++) {
                        String line = lines[i].trim();
                        if (line.isEmpty())
                                continue;
                        String[] fields = line.split(";", -1);
                        if (fields.length < 12)
                                continue;
                        String currency = fields[2]; // BBK_STD_CURRENCY
                        String timePeriod = fields[7]; // TIME_PERIOD
                        String obsValue = fields[8]; // OBS_VALUE
                        String seriesKey = fields[11]; // BBK_ID
                        rows.add(new FxCsvRateRow(currency, timePeriod, obsValue, seriesKey));
                }
                return rows;
        }

        private List<FxCsvRateRow> fallbackFullLoad(Exception ex) {
                LOGGER.error("Bundesbank API unavailable for full load: {}", ex.getMessage());
                throw new BundesbankApiException("Bundesbank API unavailable during full load", ex);
        }

        private List<FxCsvRateRow> fallbackDelta(LocalDate startDate, LocalDate endDate, Exception ex) {
                LOGGER.error("Bundesbank API unavailable for delta {} to {}: {}", startDate, endDate, ex.getMessage());
                throw new BundesbankApiException(
                                "Bundesbank API unavailable during delta load for " + startDate + " to " + endDate, ex);
        }
}
