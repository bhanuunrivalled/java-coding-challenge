package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateProvider;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.util.List;

@Configuration
public class FxCsvImportConfiguration {

    private static final String LOAD_TYPE_FULL = "full";

    @Bean
    @StepScope
    ItemReader<FxCsvRateRow> fxCsvReader(
            FxRateProvider fxRateProvider,
            @Value("#{jobParameters['loadType']}") String loadType,
            @Value("#{jobParameters['startDate']}") String startDate,
            @Value("#{jobParameters['endDate']}") String endDate) {

        List<FxCsvRateRow> rows = LOAD_TYPE_FULL.equals(loadType)
                ? fxRateProvider.fetchFullLoad()
                : fxRateProvider.fetchDelta(LocalDate.parse(startDate), LocalDate.parse(endDate));

        return new ListItemReader<>(rows);
    }

    @Bean
    JpaItemWriter<FxRateEntity> fxRateItemWriter(EntityManagerFactory entityManagerFactory) {
        JpaItemWriter<FxRateEntity> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
