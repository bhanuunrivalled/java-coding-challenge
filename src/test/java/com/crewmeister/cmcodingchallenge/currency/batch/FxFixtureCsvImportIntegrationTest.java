package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.BundesbankClient;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = FxFixtureCsvImportIntegrationTest.FixtureClientConfiguration.class)
@ActiveProfiles("test")
class FxFixtureCsvImportIntegrationTest {

    @TestConfiguration
    static class FixtureClientConfiguration {
        @Bean
        @Primary
        BundesbankClient bundesbankClient(@Value("${bundesbank.fixture.path}") Resource fixtureCsv) {
            return new BundesbankClient(RestClient.builder()) {
                @Override
                public String fetchFullLoadCsv() {
                    return readFixtureCsv(fixtureCsv);
                }

                @Override
                public String fetchDeltaCsv(LocalDate date) {
                    return readFixtureCsv(fixtureCsv);
                }

                private String readFixtureCsv(Resource fixtureCsvResource) {
                    try {
                        return StreamUtils.copyToString(fixtureCsvResource.getInputStream(), StandardCharsets.UTF_8);
                    } catch (IOException ex) {
                        throw new IllegalStateException("Could not read fixture CSV", ex);
                    }
                }
            };
        }
    }

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    @Qualifier("fullLoadJob")
    private Job fullLoadJob;

    @Autowired
    private FxRateRepository fxRateRepository;

    @BeforeEach
    void clearRates() {
        fxRateRepository.deleteAllInBatch();
    }

    @Test
    void should_import_fixture_rows_into_table_and_skip_dot_rows() throws Exception {
        jobLauncher.run(fullLoadJob, new JobParametersBuilder()
                .addString("loadType", "full")
                .addString("date", "2026-01-31")
                .toJobParameters());

        FxRateEntity jan02 = fxRateRepository.findByCurrencyAndRateDate("AUD", LocalDate.parse("2026-01-02"))
                .orElseThrow(() -> new AssertionError("Expected row for AUD 2026-01-02"));
        assertEquals(1.7508, jan02.getRate(), 0.000001);
        assertEquals("BBEX3.D.AUD.EUR.BB.AC.000", jan02.getSeriesKey());

        FxRateEntity jan05 = fxRateRepository.findByCurrencyAndRateDate("AUD", LocalDate.parse("2026-01-05"))
                .orElseThrow(() -> new AssertionError("Expected row for AUD 2026-01-05"));
        assertEquals(1.7492, jan05.getRate(), 0.000001);

        FxRateEntity jan06 = fxRateRepository.findByCurrencyAndRateDate("AUD", LocalDate.parse("2026-01-06"))
                .orElseThrow(() -> new AssertionError("Expected row for AUD 2026-01-06"));
        assertEquals(1.7422, jan06.getRate(), 0.000001);

        assertFalse(
                fxRateRepository.findByCurrencyAndRateDate("AUD", LocalDate.parse("2026-01-01")).isPresent(),
                "Dot-value rows must be skipped"
        );
        assertTrue(fxRateRepository.count() > 3, "Fixture should import multiple valid rows");
    }
}
