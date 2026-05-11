package com.crewmeister.cmcodingchallenge.currency.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DeltaLoadResponse(
        String status,
        String startDate,
        String endDate,
        String reason,
        String message) {
    public static DeltaLoadResponse success(String startDate, String endDate) {
        return new DeltaLoadResponse("completed", startDate, endDate, null, null);
    }

    public static DeltaLoadResponse skippedEmpty() {
        return new DeltaLoadResponse("skipped", null, null,
                "Database empty — full load required first", null);
    }

    public static DeltaLoadResponse upToDate(String maxRateDate) {
        return new DeltaLoadResponse("up_to_date", null, null,
                "Data already up to date (latest: " + maxRateDate + ")", null);
    }

    public static DeltaLoadResponse error(String message) {
        return new DeltaLoadResponse("error", null, null, null, message);
    }
}
