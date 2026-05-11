package com.crewmeister.cmcodingchallenge.currency.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "EUR conversion result")
public record ConversionResponse(
        @Schema(description = "Currency converted from", example = "USD")
        String sourceCurrency,
        @Schema(description = "Amount in the source currency", example = "100.0")
        double sourceAmount,
        @Schema(description = "Date requested for the conversion", example = "2026-05-01")
        LocalDate conversionDate,
        @Schema(description = "Converted EUR amount", example = "90.90909090909091")
        double eurEquivalent) {
}
