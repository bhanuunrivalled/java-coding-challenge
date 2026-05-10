package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class FxJobDefinitions {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxJobDefinitions.class);
    private static final int CHUNK_SIZE = 1000;

    private final FxRateRepository fxRateRepository;

    public FxJobDefinitions(FxRateRepository fxRateRepository) {
        this.fxRateRepository = fxRateRepository;
    }

    @Bean
    Step clearFxRatesStep(JobRepository jobRepository, PlatformTransactionManager tx) {
        return new StepBuilder("clearFxRatesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    fxRateRepository.deleteAllInBatch();
                    LOGGER.info("Cleared existing FX rates before full load");
                    return RepeatStatus.FINISHED;
                }, tx)
                .build();
    }

    @Bean
    Step fullLoadChunkStep(JobRepository jobRepository, PlatformTransactionManager tx,
            ItemStreamReader<FxCsvRateRow> fxCsvReader,
            ItemProcessor<FxCsvRateRow, FxRateEntity> fxRateItemProcessor,
            JpaItemWriter<FxRateEntity> fxRateItemWriter) {
        return new StepBuilder("fullLoadChunkStep", jobRepository)
                .<FxCsvRateRow, FxRateEntity>chunk(CHUNK_SIZE, tx)
                .reader(fxCsvReader).processor(fxRateItemProcessor).writer(fxRateItemWriter)
                .build();
    }

    @Bean
    Step deltaChunkStep(JobRepository jobRepository, PlatformTransactionManager tx,
            ItemStreamReader<FxCsvRateRow> fxCsvReader,
            ItemProcessor<FxCsvRateRow, FxRateEntity> fxRateItemProcessor,
            JpaItemWriter<FxRateEntity> fxRateItemWriter) {
        return new StepBuilder("deltaChunkStep", jobRepository)
                .<FxCsvRateRow, FxRateEntity>chunk(CHUNK_SIZE, tx)
                .reader(fxCsvReader).processor(fxRateItemProcessor).writer(fxRateItemWriter)
                .build();
    }

    @Bean
    Job fullLoadJob(JobRepository jobRepository, Step clearFxRatesStep, Step fullLoadChunkStep) {
        return new JobBuilder("fullLoadJob", jobRepository)
                .start(clearFxRatesStep).next(fullLoadChunkStep).build();
    }

    @Bean
    Job deltaLoadJob(JobRepository jobRepository, Step deltaChunkStep) {
        return new JobBuilder("deltaLoadJob", jobRepository)
                .start(deltaChunkStep).build();
    }
}
