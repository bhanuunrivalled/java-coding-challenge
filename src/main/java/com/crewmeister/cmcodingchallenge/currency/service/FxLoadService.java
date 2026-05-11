package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Orchestrates FX rate loading — both full and delta.
 * Single place for guard logic, job launching, and cache eviction.
 */
@Service
public class FxLoadService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FxLoadService.class);

    private final FxRateRepository fxRateRepository;
    private final JobLauncher jobLauncher;
    private final Job fullLoadJob;
    private final Job deltaLoadJob;
    private final CurrencyQueryService currencyQueryService;
    private final Clock clock;

    public FxLoadService(FxRateRepository fxRateRepository,
            JobLauncher jobLauncher,
            @Qualifier("fullLoadJob") Job fullLoadJob,
            @Qualifier("deltaLoadJob") Job deltaLoadJob,
            CurrencyQueryService currencyQueryService,
            Clock clock) {
        this.fxRateRepository = fxRateRepository;
        this.jobLauncher = jobLauncher;
        this.fullLoadJob = fullLoadJob;
        this.deltaLoadJob = deltaLoadJob;
        this.currencyQueryService = currencyQueryService;
        this.clock = clock;
    }

    /**
     * On startup: run full load only when enabled and the store is empty.
     * Never crashes the application — logs the error and lets the service start.
     */
    public void executeStartupFullLoadIfEnabledAndEmpty(boolean fullLoadOnStartup) {
        if (!fullLoadOnStartup) {
            LOGGER.info("Startup full load disabled by property fx.batch.full-load-on-startup=false");
            return;
        }
        if (fxRateRepository.count() > 0) {
            LOGGER.info("FX rate store already populated - skipping full load");
            return;
        }
        LOGGER.info("FX rate store is empty - starting full load");
        try {
            executeFullLoad();
        } catch (Exception ex) {
            LOGGER.error("Startup full load failed — service will start without data. " +
                    "Trigger manually via POST /api/batch/delta once the API is available. Error: {}",
                    ex.getMessage());
        }
    }

    /** Runs the full load job. Clears existing data and imports all history. */
    private void executeFullLoad() {
        LOGGER.info("Starting full load");
        JobParameters params = new JobParametersBuilder()
                .addString("loadType", "full")
                .addString("startDate", LocalDate.EPOCH.toString())
                .addString("endDate", LocalDate.now(clock).toString())
                .toJobParameters();
        launchJob(fullLoadJob, params, "Full load");
    }

    /** Runs the delta load with guard checks and automatic date window. */
    public DeltaLoadResult executeDeltaLoad() {
        Optional<LocalDate> maxRateDate = fxRateRepository.findMaxRateDate();

        if (maxRateDate.isEmpty()) {
            LOGGER.warn("Delta load skipped — no existing rates. Full load required first.");
            return DeltaLoadResult.skippedEmpty();
        }

        LocalDate startDate = maxRateDate.get().plusDays(1);
        LocalDate endDate = LocalDate.now(clock);

        if (startDate.isAfter(endDate)) {
            LOGGER.info("Delta load skipped — already up to date (latest: {})", maxRateDate.get());
            return DeltaLoadResult.skippedUpToDate(maxRateDate.get());
        }

        LOGGER.info("Starting delta load from {} to {}", startDate, endDate);
        JobParameters params = new JobParametersBuilder()
                .addString("loadType", "delta")
                .addString("startDate", startDate.toString())
                .addString("endDate", endDate.toString())
                .toJobParameters();
        launchJob(deltaLoadJob, params, "Delta load");
        return DeltaLoadResult.success(startDate, endDate);
    }

    private void launchJob(Job job, JobParameters params, String description) {
        try {
            JobExecution execution = jobLauncher.run(job, params);
            if (execution.getStatus() == BatchStatus.COMPLETED) {
                currencyQueryService.evictCurrenciesCache();
                LOGGER.info("{} completed successfully", description);
            } else {
                throw new RuntimeException(description + " finished with status: " + execution.getStatus());
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            LOGGER.error("{} failed: {}", description, ex.getMessage(), ex);
            throw new RuntimeException(description + " failed: " + ex.getMessage(), ex);
        }
    }
}
