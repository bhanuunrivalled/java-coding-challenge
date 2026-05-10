package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FxRateItemProcessorTest {

    private final FxRateItemProcessor processor = new FxRateItemProcessor();

    @Test
    void should_map_row_to_entity_when_row_is_valid() {
        FxCsvRateRow row = new FxCsvRateRow("USD", "2024-01-01", "1.1", "BBEX3.D.USD.EUR.BB.AC.000");

        FxRateEntity result = processor.process(row);

        assertNotNull(result);
        assertEquals("USD", result.getCurrency());
        assertEquals(LocalDate.parse("2024-01-01"), result.getRateDate());
        assertEquals(1.1, result.getRate());
        assertEquals("BBEX3.D.USD.EUR.BB.AC.000", result.getSeriesKey());
    }

    @Test
    void should_skip_row_when_value_is_missing_dot() {
        FxCsvRateRow row = new FxCsvRateRow("USD", "2024-01-01", ".", "BBEX3.D.USD.EUR.BB.AC.000");

        FxRateEntity result = processor.process(row);

        assertNull(result);
    }

    @Test
    void should_skip_row_when_value_or_date_is_invalid() {
        FxCsvRateRow badRate = new FxCsvRateRow("USD", "2024-01-01", "abc", "BBEX3.D.USD.EUR.BB.AC.000");
        FxCsvRateRow badDate = new FxCsvRateRow("USD", "01-01-2024", "1.1", "BBEX3.D.USD.EUR.BB.AC.000");

        assertNull(processor.process(badRate));
        assertNull(processor.process(badDate));
    }
}
