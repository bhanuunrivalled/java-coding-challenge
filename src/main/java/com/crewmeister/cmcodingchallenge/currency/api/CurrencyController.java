package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.service.CurrencyQueryService;
import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;

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
public class CurrencyController {
    private final CurrencyQueryService currencyQueryService;

    public CurrencyController(CurrencyQueryService currencyQueryService) {
        this.currencyQueryService = currencyQueryService;
    }

    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getCurrencies() {
        return new ResponseEntity<List<String>>(currencyQueryService.getAvailableCurrencies(), HttpStatus.OK);
    }

    @GetMapping("/rates/{currency}")
    public ResponseEntity<FxRateResponse> getRateAtDate(
            @PathVariable String currency,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        FxRateEntity rate = currencyQueryService
                .getRateAtOrBeforeDate(currency, date)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No rate available for currency " + currency + " on or before " + date));

        FxRateResponse response = new FxRateResponse(
                currency.trim().toUpperCase(),
                date,
                rate.getRateDate(),
                rate.getRate());

        return new ResponseEntity<FxRateResponse>(response, HttpStatus.OK);
    }
}
