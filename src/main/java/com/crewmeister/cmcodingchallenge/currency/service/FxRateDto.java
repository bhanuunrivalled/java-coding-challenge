package com.crewmeister.cmcodingchallenge.currency.service;

import java.time.LocalDate;


public record FxRateDto(String currency, LocalDate rateDate, double rate) {
}
