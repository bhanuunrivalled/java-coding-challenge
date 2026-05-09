package com.crewmeister.cmcodingchallenge.currency;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class BundesbankClient {
    private final RestClient.Builder restClientBuilder;

    public BundesbankClient(RestClient.Builder restClientBuilder) {
        this.restClientBuilder = restClientBuilder;
    }

    public List<String> fetchAvailableCurrencies() {
        // TODO implement /data/BBEX3/D..EUR.BB.AC.000?detail=serieskeyonly&format=sdmx_csv
        return List.of();
    }
}
