package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FxRateItemProcessor — no Spring context needed.
 * Tests the CSV row → FxRateEntity transformation logic in isolation.
 */
class FxRateItemProcessorTest {

    private final FxRateItemProcessor processor = new FxRateItemProcessor();

    @Test
    void should_map_row_to_entity_when_row_is_valid() {
        FxCsvRateRow row = new FxCsvRateRow("AUD", "2026-01-02", "1.7508", "BBEX3.D.AUD.EUR.BB.AC.000");

        FxRateEntity result = processor.process(row);

        assertNotNull(result);
        assertEquals("AUD", result.getCurrency());
        assertEquals(LocalDate.of(2026, 1, 2), result.getRateDate());
        assertEquals(1.7508, result.getRate());
        assertEquals("BBEX3.D.AUD.EUR.BB.AC.000", result.getSeriesKey());
    }

    @Test
    void should_skip_row_when_obs_value_is_dot() {
        FxCsvRateRow row = new FxCsvRateRow("AUD", "2026-01-01", ".", "BBEX3.D.AUD.EUR.BB.AC.000");

        assertNull(processor.process(row));
    }

    @Test
    void should_skip_row_when_item_is_null() {
        assertNull(processor.process(null));
    }

    @Test
    void should_skip_row_when_currency_is_blank() {
        FxCsvRateRow row = new FxCsvRateRow("  ", "2026-01-02", "1.7508", "BBEX3.D.AUD.EUR.BB.AC.000");

        assertNull(processor.process(row));
    }

    @Test
    void should_skip_row_when_date_is_invalid() {
        FxCsvRateRow row = new FxCsvRateRow("AUD", "not-a-date", "1.7508", "BBEX3.D.AUD.EUR.BB.AC.000");

        assertNull(processor.process(row));
    }

    @Test
    void should_skip_row_when_rate_is_not_a_number() {
        FxCsvRateRow row = new FxCsvRateRow("AUD", "2026-01-02", "abc", "BBEX3.D.AUD.EUR.BB.AC.000");

        assertNull(processor.process(row));
    }

    @Test
    void should_skip_row_when_obs_value_is_blank() {
        FxCsvRateRow row = new FxCsvRateRow("AUD", "2026-01-02", "  ", "BBEX3.D.AUD.EUR.BB.AC.000");

        assertNull(processor.process(row));
    }

    @Test
    void should_trim_whitespace_from_fields() {
        FxCsvRateRow row = new FxCsvRateRow(" AUD ", " 2026-01-02 ", " 1.7508 ", " BBEX3.D.AUD.EUR.BB.AC.000 ");

        FxRateEntity result = processor.process(row);

        assertNotNull(result);
        assertEquals("AUD", result.getCurrency());
        assertEquals(LocalDate.of(2026, 1, 2), result.getRateDate());
    }
}
