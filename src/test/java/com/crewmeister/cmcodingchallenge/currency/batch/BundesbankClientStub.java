package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.BundesbankClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

/**
 * Replaces BundesbankClient in tests — reads from fixture CSV, no network
 * calls.
 */
@TestConfiguration
public class BundesbankClientStub {

    @Value("${bundesbank.fixture.path}")
    private Resource fixtureCsv;

    @Bean
    @Primary
    BundesbankClient bundesbankClient() {
        return new BundesbankClient(null) {
            @Override
            public String fetchFullLoadCsv() {
                return readFixture();
            }

            @Override
            public String fetchDeltaCsv(LocalDate date) {
                return readFixture();
            }

            private String readFixture() {
                try {
                    return fixtureCsv.getContentAsString(StandardCharsets.UTF_8);
                } catch (IOException e) {
                    throw new IllegalStateException("Could not read fixture CSV", e);
                }
            }
        };
    }
}
