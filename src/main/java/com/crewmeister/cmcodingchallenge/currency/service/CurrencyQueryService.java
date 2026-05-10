package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class CurrencyQueryService {
    private final FxRateRepository fxRateRepository;

    public CurrencyQueryService(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    public List<String> getAvailableCurrencies() {
        return fxRateRepository.findDistinctCurrencies();
    }

    public Optional<FxRateDto> getRateAtOrBeforeDate(String currency, LocalDate requestedDate) {
        return fxRateRepository.findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(
                currency.trim().toUpperCase(),
                requestedDate)
                .map(entity -> new FxRateDto(entity.getCurrency(), entity.getRateDate(), entity.getRate()));
    }

    public List<FxRateDto> getAllRatesByCurrency(String currency) {
        // FIXME PAGING
        return fxRateRepository.findByCurrencyOrderByRateDateAsc(currency.trim().toUpperCase()).stream()
                .map(entity -> new FxRateDto(entity.getCurrency(), entity.getRateDate(), entity.getRate()))
                .toList();
    }

    public Optional<Double> convertToEur(String currency, double amount, LocalDate conversionDate) {
        return getRateAtOrBeforeDate(currency, conversionDate)
                .map(rate -> amount / rate.rate());
    }
}
