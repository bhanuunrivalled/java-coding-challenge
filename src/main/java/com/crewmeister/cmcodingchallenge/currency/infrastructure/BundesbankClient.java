package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BundesbankClient {
    private static final String FLOW_REF = "BBEX3";
    private static final String FULL_LOAD_KEY = "D..EUR.BB.AC.000";
    private static final String SDMX_JSON_ACCEPT = "application/vnd.sdmx.data+json;version=1.0.0";

    private final RestClient restClient;

    private final RestClient.Builder restClientBuilder;

    public BundesbankClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
        this.restClient = restClientBuilder
                .baseUrl("https://api.statistiken.bundesbank.de/rest")
                .defaultHeader(HttpHeaders.ACCEPT, SDMX_JSON_ACCEPT)
                .build();
    }

    public String fetchFullLoadJson() {
        return restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/data/{flowRef}/{key}")
                        .queryParam("detail", "full")
                        .build(FLOW_REF, FULL_LOAD_KEY))
                .retrieve()
                .body(String.class);
    }
}
