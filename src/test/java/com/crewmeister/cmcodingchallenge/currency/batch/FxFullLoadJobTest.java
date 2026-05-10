package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FxFullLoadJobTest {

    @Autowired
    FxBatchConfiguration batchConfiguration;

    @Autowired
    FxRateRepository fxRateRepository;

    @Test
    void should_load_rates_from_fixture_when_table_is_empty() {
        fxRateRepository.deleteAllInBatch();

        batchConfiguration.runFullLoadIfEmpty();

        // fixture has 3 valid rows (4th row has "." obsValue — skipped by processor)
        assertEquals(3, fxRateRepository.count());
    }

    @Test
    void should_skip_full_load_when_table_already_has_data() {
        assertTrue(fxRateRepository.count() > 0, "Precondition: table must have data");
        long countBefore = fxRateRepository.count();

        batchConfiguration.runFullLoadIfEmpty();

        assertEquals(countBefore, fxRateRepository.count());
    }
}
