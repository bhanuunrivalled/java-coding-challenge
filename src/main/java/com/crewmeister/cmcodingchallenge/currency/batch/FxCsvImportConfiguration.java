package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.BundesbankClient;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
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

    /**
     * Reader is step-scoped so the HTTP call to Bundesbank happens at job execution
     * time, not at Spring context startup. loadType="full" fetches all history;
     * loadType="delta" fetches only the date passed as a job parameter.
     */
    @Bean
    @StepScope
    FlatFileItemReader<FxCsvRateRow> fxCsvReader(
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
                "BBK_DECIMALS", "BBK_ID");

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
