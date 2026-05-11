package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FxRateRepository extends JpaRepository<FxRateEntity, Long> {
    @Query("select distinct f.currency from FxRateEntity f order by f.currency asc")
    List<String> findDistinctCurrencies();

    @Query("SELECT MAX(f.rateDate) FROM FxRateEntity f")
    Optional<LocalDate> findMaxRateDate();

    Optional<FxRateEntity> findByCurrencyAndRateDate(String currency, LocalDate rateDate);

    Optional<FxRateEntity> findTopByCurrencyAndRateDateLessThanEqualOrderByRateDateDesc(String currency,
            LocalDate rateDate);

    Page<FxRateEntity> findByCurrencyOrderByRateDateAsc(String currency, Pageable pageable);
}
