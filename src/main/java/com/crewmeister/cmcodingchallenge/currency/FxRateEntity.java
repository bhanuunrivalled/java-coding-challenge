package com.crewmeister.cmcodingchallenge.currency;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDate;

@Entity
@Table(name = "fx_rate", uniqueConstraints = @UniqueConstraint(columnNames = {"currency", "rateDate"}))
public class FxRateEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String seriesKey;
    private String currency;
    private LocalDate rateDate;
    private double rate;

    protected FxRateEntity() {
    }

    public FxRateEntity(String seriesKey, String currency, LocalDate rateDate, double rate) {
        this.seriesKey = seriesKey;
        this.currency = currency;
        this.rateDate = rateDate;
        this.rate = rate;
    }
}
