package com.crewmeister.cmcodingchallenge.currency.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "FX rate returned by the API")
public record FxRateResponse(
        @Schema(description = "Currency code", example = "AUD")
        String currency,
        @Schema(description = "Date requested by the caller", example = "2026-05-01")
        LocalDate requestedDate,
        @Schema(
                description = "Actual rate date used. May be earlier than requestedDate when no rate exists for the requested date.",
                example = "2026-04-30")
        LocalDate effectiveRateDate,
        @Schema(description = "Exchange rate against EUR", example = "1.6393")
        double rate) {
}
