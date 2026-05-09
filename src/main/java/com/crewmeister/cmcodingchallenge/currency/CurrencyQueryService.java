package com.crewmeister.cmcodingchallenge.currency;

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
