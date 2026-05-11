package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.BundesbankClient;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import com.crewmeister.cmcodingchallenge.currency.service.CurrencyQueryService;
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
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

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
        BundesbankClient bundesbankClient(@Value("${bundesbank.fixture.path}") Resource fixtureCsv,
                @Value("${bundesbank.api.base-url}") String baseUrl,
                @Value("${bundesbank.api.connect-timeout}") java.time.Duration connectTimeout,
                @Value("${bundesbank.api.read-timeout}") java.time.Duration readTimeout) {
            return new BundesbankClient(RestClient.builder(), baseUrl, connectTimeout, readTimeout) {
                @Override
                public List<FxCsvRateRow> fetchFullLoad() {
                    return parseFixtureCsv(fixtureCsv);
                }

                @Override
                public List<FxCsvRateRow> fetchDelta(LocalDate startDate, LocalDate endDate) {
                    return parseFixtureCsv(fixtureCsv);
                }

                private List<FxCsvRateRow> parseFixtureCsv(Resource fixtureCsvResource) {
                    try {
                        String csv = StreamUtils.copyToString(fixtureCsvResource.getInputStream(),
                                StandardCharsets.UTF_8);
                        String[] lines = csv.split("\n");
                        List<FxCsvRateRow> rows = new java.util.ArrayList<>();
                        for (int i = 1; i < lines.length; i++) {
                            String line = lines[i].trim();
                            if (line.isEmpty())
                                continue;
                            String[] fields = line.split(";", -1);
                            if (fields.length < 12)
                                continue;
                            rows.add(new FxCsvRateRow(fields[2], fields[7], fields[8], fields[11]));
                        }
                        return rows;
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

    @Autowired
    private CurrencyQueryService currencyQueryService;

    @Autowired
    private FxBatchConfiguration fxBatchConfiguration;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void clearRates() {
        fxRateRepository.deleteAllInBatch();
        if (cacheManager.getCache(CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE) != null) {
            cacheManager.getCache(CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE).clear();
        }
    }

    @Test
    void should_import_fixture_rows_into_table_and_skip_dot_rows() throws Exception {
        jobLauncher.run(fullLoadJob, new JobParametersBuilder()
                .addString("loadType", "full")
                .addString("startDate", "1970-01-01")
                .addString("endDate", "2026-01-31")
                .addLong("runId", 1L)
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
                "Dot-value rows must be skipped");
        assertTrue(fxRateRepository.count() > 3, "Fixture should import multiple valid rows");
    }

    @Test
    void should_skip_delta_load_when_database_is_empty() {
        // DB is empty after @BeforeEach — delta load should be skipped gracefully
        fxBatchConfiguration.runDeltaLoad();
        // No exception = guard worked correctly (DeltaLoadService returns SkippedEmpty)
        assertEquals(0, fxRateRepository.count(), "No rates should be loaded when guard skips");
    }
}
