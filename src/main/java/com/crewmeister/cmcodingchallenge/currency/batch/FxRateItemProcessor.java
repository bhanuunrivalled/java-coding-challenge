package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Component
public class FxRateItemProcessor implements ItemProcessor<FxCsvRateRow, FxRateEntity> {
    private static final Logger LOGGER = LoggerFactory.getLogger(FxRateItemProcessor.class);

    @Override
    public FxRateEntity process(FxCsvRateRow item) {
        if (item == null || item.currency() == null || item.timePeriod() == null || item.obsValue() == null) {
            LOGGER.warn("Skipping malformed FX CSV row: missing required fields");
            return null;
        }

        String currency = item.currency().trim();
        String timePeriod = item.timePeriod().trim();
        String obsValue = item.obsValue().trim();
        String seriesKey = item.seriesKey() == null ? "" : item.seriesKey().trim();

        if (currency.isEmpty() || timePeriod.isEmpty() || obsValue.isEmpty() || ".".equals(obsValue)) {
            return null;
        }

        try {
            return new FxRateEntity(seriesKey, currency, LocalDate.parse(timePeriod), Double.parseDouble(obsValue));
        } catch (DateTimeParseException | NumberFormatException ex) {
            LOGGER.warn(
                    "Skipping malformed FX CSV row: currency={}, timePeriod={}, obsValue={}, seriesKey={}",
                    currency,
                    timePeriod,
                    obsValue,
                    seriesKey
            );
            return null;
        }
    }
}
