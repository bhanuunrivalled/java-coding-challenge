package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.BundesbankClient;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@Configuration
public class FxCsvImportConfiguration {

    private static final String LOAD_TYPE_FULL = "full";
    private static final int DISABLED = 0;

    /**
     * When bundesbank.sample-size > 0 (dev profile), wraps the reader in a
     * SamplingFxCsvReader that picks N random rows from the real API response.
     * This lets dev verify the real CSV format without loading all history.
     * Set to 0 (default) to disable sampling and load all rows.
     */
    @Value("${bundesbank.sample-size:0}")
    private int sampleSize;

    @Bean
    @StepScope
    ItemStreamReader<FxCsvRateRow> fxCsvReader(
            BundesbankClient bundesbankClient,
            @Value("#{jobParameters['loadType']}") String loadType,
            @Value("#{jobParameters['date']}") String date) {

        String csv = LOAD_TYPE_FULL.equals(loadType)
                ? bundesbankClient.fetchFullLoadCsv()
                : bundesbankClient.fetchDeltaCsv(LocalDate.parse(date));

        FlatFileItemReader<FxCsvRateRow> reader = new FlatFileItemReader<>();
        reader.setResource(new ByteArrayResource(csv.getBytes(StandardCharsets.UTF_8)));
        reader.setLinesToSkip(1);
        reader.setLineMapper(fxCsvLineMapper());

        if (sampleSize > DISABLED) {
            return new SamplingFxCsvReader(reader, sampleSize);
        }
        return reader;
    }

    @Bean
    LineMapper<FxCsvRateRow> fxCsvLineMapper() {
        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setDelimiter(";");
        tokenizer.setNames(
            "DATAFLOW", "BBK_STD_FREQ", "BBK_STD_CURRENCY",
            "BBK_ERX_PARTNER_CURRENCY", "BBK_ERX_SERIES_TYPE",
            "BBK_ERX_RATE_TYPE", "BBK_ERX_SUFFIX",
            "TIME_PERIOD", "OBS_VALUE", "TIME_FORMAT",
            "BBK_DECIMALS", "BBK_ID", "BBK_UNIT",
            "BBK_UNIT_MULT", "BBK_TITLE", "WEB_CATEGORY",
                "BBK_COMM_GEN", "BBK_COMM_SRC", "OBS_STATUS",
                "BBK_DIFF", "EXTRA_EMPTY");

        DefaultLineMapper<FxCsvRateRow> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(fieldSet -> new FxCsvRateRow(
                fieldSet.readString("BBK_STD_CURRENCY"),
                fieldSet.readString("TIME_PERIOD"),
                fieldSet.readString("OBS_VALUE"),
                fieldSet.readString("BBK_ID")));
        return lineMapper;
    }

    @Bean
    JpaItemWriter<FxRateEntity> fxRateItemWriter(EntityManagerFactory entityManagerFactory) {
        JpaItemWriter<FxRateEntity> writer = new JpaItemWriter<>();
        writer.setEntityManagerFactory(entityManagerFactory);
        return writer;
    }
}
