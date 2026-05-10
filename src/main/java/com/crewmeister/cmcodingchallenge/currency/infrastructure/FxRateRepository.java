package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRateEntity, Long> {
    @Query("select distinct f.currency from FxRateEntity f order by f.currency asc")
    List<String> findDistinctCurrencies();

    Optional<FxRateEntity> findByCurrencyAndRateDate(String currency, LocalDate rateDate);

    Optional<FxRateEntity> findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(String currency, LocalDate rateDate);

    List<FxRateEntity> findByCurrencyOrderByRateDateAsc(String currency);
}
