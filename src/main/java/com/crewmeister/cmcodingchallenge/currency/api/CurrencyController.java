package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.service.CurrencyQueryService;
import com.crewmeister.cmcodingchallenge.currency.service.FxRateDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;

@RestController()
@RequestMapping("/api")
@Tag(name = "Currency")
public class CurrencyController {
    private final CurrencyQueryService currencyQueryService;

    public CurrencyController(CurrencyQueryService currencyQueryService) {
        this.currencyQueryService = currencyQueryService;
    }

    @Operation(summary = "List available currencies")
    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        return new ResponseEntity<List<String>>(currencyQueryService.getAvailableCurrencies(), HttpStatus.OK);
    }

    @Operation(summary = "Get an FX rate for a date, falling back to the latest previous available rate")
    @GetMapping("/rates/{currency}")
    public ResponseEntity<FxRateResponse> getRateAtDate(
            @Parameter(description = "Currency code", example = "AUD")
            @PathVariable String currency,
            @Parameter(description = "Requested rate date", example = "2026-05-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        FxRateDto rate = currencyQueryService
                .getRateAtOrBeforeDate(currency, date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No rate available for currency " + currency + " on or before " + date));

        FxRateResponse response = new FxRateResponse(
                currency.trim().toUpperCase(),
                date,
                rate.rateDate(),
                rate.rate());

        return new ResponseEntity<FxRateResponse>(response, HttpStatus.OK);
    }

    @Operation(summary = "Get all known FX rates for a currency")
    @GetMapping("/rates/{currency}/history")
    public ResponseEntity<List<FxRateResponse>> getRatesHistory(
            @Parameter(description = "Currency code", example = "AUD")
            @PathVariable String currency) {
        List<FxRateDto> rates = currencyQueryService.getAllRatesByCurrency(currency);
        if (rates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No rates found for currency " + currency);
        }
        List<FxRateResponse> responses = rates.stream()
                .map(r -> new FxRateResponse(r.currency(), r.rateDate(), r.rateDate(), r.rate()))
                .toList();
        return new ResponseEntity<>(responses, HttpStatus.OK);
    }

    @Operation(summary = "Convert an amount from a currency to EUR")
    @GetMapping("/rates/convert")
    public ResponseEntity<ConversionResponse> convertToEur(
            @Parameter(description = "Currency code", example = "USD")
            @RequestParam String currency,
            @Parameter(description = "Amount in the source currency", example = "100.0")
            @RequestParam double amount,
            @Parameter(description = "Requested conversion date", example = "2026-05-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        Double eurEquivalent = currencyQueryService.convertToEur(currency, amount, date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No rate available for currency " + currency + " on or before " + date));

        ConversionResponse response = new ConversionResponse(
                currency.trim().toUpperCase(),
                amount,
                date,
                eurEquivalent);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
