package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FxRateRepository extends JpaRepository<FxRateEntity, Long> {
    @Query("select distinct f.currency from FxRateEntity f order by f.currency asc")
    List<String> findDistinctCurrencies();
}
