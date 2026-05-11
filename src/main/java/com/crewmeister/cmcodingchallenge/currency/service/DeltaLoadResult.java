package com.crewmeister.cmcodingchallenge.currency.service;

import java.time.LocalDate;

/**
 * Outcome of a delta load attempt.
 */
public record DeltaLoadResult(Status status, LocalDate startDate, LocalDate endDate, String reason) {

    public enum Status {
        SUCCESS,
        SKIPPED_EMPTY,
        SKIPPED_UP_TO_DATE
    }

    public static DeltaLoadResult success(LocalDate startDate, LocalDate endDate) {
        return new DeltaLoadResult(Status.SUCCESS, startDate, endDate, null);
    }

    public static DeltaLoadResult skippedEmpty() {
        return new DeltaLoadResult(Status.SKIPPED_EMPTY, null, null, "Database empty — full load required first");
    }

    public static DeltaLoadResult skippedUpToDate(LocalDate maxRateDate) {
        return new DeltaLoadResult(Status.SKIPPED_UP_TO_DATE, null, null,
                "Data already up to date (latest: " + maxRateDate + ")");
    }
}
