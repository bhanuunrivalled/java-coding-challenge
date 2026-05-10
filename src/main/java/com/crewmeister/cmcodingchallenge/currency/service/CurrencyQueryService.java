package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CurrencyQueryService {
    private final FxRateRepository fxRateRepository;

    public CurrencyQueryService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    public List<String> getAvailableCurrencies() {
        return fxRateRepository.findDistinctCurrencies();
    }
}
