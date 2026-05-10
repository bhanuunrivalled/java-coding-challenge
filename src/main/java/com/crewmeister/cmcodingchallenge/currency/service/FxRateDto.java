package com.crewmeister.cmcodingchallenge.currency.service;

import java.time.LocalDate;

/**
 * Simple boundary DTO for FX rate data crossing from service to adapter layers.
 */
public record FxRateDto(String currency, LocalDate rateDate, double rate) {
}
