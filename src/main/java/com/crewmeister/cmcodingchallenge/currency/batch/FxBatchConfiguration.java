package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.service.CurrencyQueryService;
import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
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
    private final CacheManager cacheManager;

    @Value("${fx.batch.full-load-on-startup}")
    private boolean fullLoadOnStartup;

    public FxBatchConfiguration(JobLauncher jobLauncher,
            @Qualifier("fullLoadJob") Job fullLoadJob,
            @Qualifier("deltaLoadJob") Job deltaLoadJob,
            FxRateRepository fxRateRepository,
            CacheManager cacheManager) {
        this.jobLauncher = jobLauncher;
        this.fullLoadJob = fullLoadJob;
        this.deltaLoadJob = deltaLoadJob;
        this.fxRateRepository = fxRateRepository;
        this.cacheManager = cacheManager;
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
            JobExecution execution = jobLauncher.run(job, params);
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                evictAvailableCurrenciesCache(job.getName());
            } else {
                LOGGER.warn("Batch job '{}' finished with status {} - cache not evicted",
                        job.getName(), execution.getStatus());
            }
        } catch (Exception ex) {
            LOGGER.error("Batch job '{}' failed: {}", job.getName(), ex.getMessage(), ex);
        }
    }

    private void evictAvailableCurrenciesCache(String jobName) {
        Cache cache = cacheManager.getCache(CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE);
        if (cache == null) {
            LOGGER.warn("Cache '{}' is not configured - skipping eviction after '{}'",
                    CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE, jobName);
            return;
        }
        cache.clear();
        LOGGER.info("Evicted cache '{}' after successful '{}'",
                CurrencyQueryService.AVAILABLE_CURRENCIES_CACHE, jobName);
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
