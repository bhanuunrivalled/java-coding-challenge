package com.crewmeister.cmcodingchallenge.currency.api;

import java.time.LocalDate;

public record FxRateResponse(
        String currency,
        LocalDate requestedDate,
        LocalDate effectiveRateDate,
        double rate) {
}
