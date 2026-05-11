package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import com.crewmeister.cmcodingchallenge.currency.batch.FxCsvRateRow;

import java.time.LocalDate;
import java.util.List;

/**
 * Abstraction for fetching FX rate data from an external source.
 * Each implementation handles its own data format and parsing.
 * Current implementation: BundesbankClient.
 */
public interface FxRateProvider {

    List<FxCsvRateRow> fetchFullLoad();

    List<FxCsvRateRow> fetchDelta(LocalDate startDate, LocalDate endDate);
}
