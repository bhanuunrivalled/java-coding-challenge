package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDate;

@Configuration
@EnableScheduling
public class FxBatchConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxBatchConfiguration.class);

    private final JobLauncher jobLauncher;
    private final Job fullLoadJob;
    private final Job deltaLoadJob;
    private final FxRateRepository fxRateRepository;

    @Value("${fx.batch.full-load-on-startup}")
    private boolean fullLoadOnStartup;

    public FxBatchConfiguration(JobLauncher jobLauncher,
            @Qualifier("fullLoadJob") Job fullLoadJob,
            @Qualifier("deltaLoadJob") Job deltaLoadJob,
            FxRateRepository fxRateRepository) {
        this.jobLauncher = jobLauncher;
        this.fullLoadJob = fullLoadJob;
        this.deltaLoadJob = deltaLoadJob;
        this.fxRateRepository = fxRateRepository;
    }

    /** On startup: full load only if table is empty — once, ever. */
    @EventListener(ApplicationReadyEvent.class)
    public void runFullLoadIfEmpty() {
        LOGGER.info("full load value on startup {}",fullLoadOnStartup);
        if (!fullLoadOnStartup) {
            LOGGER.info("Startup full load disabled by property fx.batch.full-load-on-startup=false");
            return;
        }
        if (fxRateRepository.count() > 0) {
            LOGGER.info("FX rate store already populated — skipping full load");
            return;
        }
        LOGGER.info("FX rate store is empty — starting full load");
        launchJob(fullLoadJob, fullLoadParams());
    }

    /** Daily delta at 16:30 CET on weekdays — ECB publishes at ~16:00 CET. */
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
}
