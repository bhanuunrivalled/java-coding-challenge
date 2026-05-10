package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.domain.FxRateEntity;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

@Configuration
@EnableScheduling
public class FxBatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxBatchConfiguration.class);
    private static final int CHUNK_SIZE = 1000;

    private final JobLauncher jobLauncher;
    private final Job fullLoadJob;
    private final Job deltaLoadJob;
    private final FxRateRepository fxRateRepository;

    public FxBatchConfiguration(JobLauncher jobLauncher,
            Job fullLoadJob,
            Job deltaLoadJob,
            FxRateRepository fxRateRepository) {
        this.jobLauncher = jobLauncher;
        this.fullLoadJob = fullLoadJob;
        this.deltaLoadJob = deltaLoadJob;
        this.fxRateRepository = fxRateRepository;
    }

    /**
     * On startup: run full load only if the table is empty (first boot ever).
     * App starts regardless — a Bundesbank failure is logged but does not crash
     * startup.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void runFullLoadIfEmpty() {
        if (fxRateRepository.count() > 0) {
            LOGGER.info("FX rate store already populated — skipping full load");
            return;
        }
        LOGGER.info("FX rate store is empty — starting full load");
        launchJob(fullLoadJob, fullLoadParams());
    }

    /**
     * Daily delta: fetch today's ECB reference rates at 16:30 CET on weekdays.
     * ECB publishes rates at ~16:00 CET every working day.
     */
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Europe/Berlin")
    public void runDeltaLoad() {
        LOGGER.info("Starting delta load for {}", LocalDate.now());
        launchJob(deltaLoadJob, deltaParams(LocalDate.now()));
    }

    private void launchJob(Job job, JobParameters params) {
        try {
            jobLauncher.run(job, params);
        } catch (Exception ex) {
            LOGGER.error("Batch job '{}' failed: {}", job.getName(), ex.getMessage(), ex);
        }
    }

    private JobParameters fullLoadParams() {
        return new JobParametersBuilder()
                .addString("loadType", "full")
                .addString("date", LocalDate.now().toString())
                .toJobParameters();
    }

    private JobParameters deltaParams(LocalDate date) {
        return new JobParametersBuilder()
                .addString("loadType", "delta")
                .addString("date", date.toString())
                .toJobParameters();
    }

    @Bean
    Step clearFxRatesStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {
        return new StepBuilder("clearFxRatesStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    fxRateRepository.deleteAllInBatch();
                    LOGGER.info("Cleared existing FX rates before full load");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Step fullLoadChunkStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<FxCsvRateRow> fxCsvReader,
            ItemProcessor<FxCsvRateRow, FxRateEntity> fxRateItemProcessor,
            JpaItemWriter<FxRateEntity> fxRateItemWriter) {
        return new StepBuilder("fullLoadChunkStep", jobRepository)
                .<FxCsvRateRow, FxRateEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(fxCsvReader)
                .processor(fxRateItemProcessor)
                .writer(fxRateItemWriter)
                .build();
    }

    @Bean
    Step deltaChunkStep(JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            FlatFileItemReader<FxCsvRateRow> fxCsvReader,
            ItemProcessor<FxCsvRateRow, FxRateEntity> fxRateItemProcessor,
            JpaItemWriter<FxRateEntity> fxRateItemWriter) {
        return new StepBuilder("deltaChunkStep", jobRepository)
                .<FxCsvRateRow, FxRateEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(fxCsvReader)
                .processor(fxRateItemProcessor)
                .writer(fxRateItemWriter)
                .build();
    }

    @Bean
    Job fullLoadJob(JobRepository jobRepository,
            Step clearFxRatesStep,
            Step fullLoadChunkStep) {
        return new JobBuilder("fullLoadJob", jobRepository)
                .start(clearFxRatesStep)
                .next(fullLoadChunkStep)
                .build();
    }

    @Bean
    Job deltaLoadJob(JobRepository jobRepository, Step deltaChunkStep) {
        return new JobBuilder("deltaLoadJob", jobRepository)
                .start(deltaChunkStep)
                .build();
    }
}
