package com.crewmeister.cmcodingchallenge.currency.batch;

public record FxCsvRateRow(
        String currency,
        String timePeriod,
        String obsValue,
        String seriesKey
) {
}
