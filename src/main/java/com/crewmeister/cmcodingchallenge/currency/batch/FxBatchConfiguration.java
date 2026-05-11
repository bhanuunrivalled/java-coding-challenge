package com.crewmeister.cmcodingchallenge.currency.batch;

import com.crewmeister.cmcodingchallenge.currency.service.FxLoadService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduling configuration for FX rate batch jobs.
 * Decides WHEN to load — the FxLoadService decides HOW.
 */
@Configuration
@EnableScheduling
public class FxBatchConfiguration {

    private final FxLoadService fxLoadService;

    @Value("${fx.batch.full-load-on-startup}")
    private boolean fullLoadOnStartup;

    public FxBatchConfiguration(FxLoadService fxLoadService) {
        this.fxLoadService = fxLoadService;
    }

    /** On startup: full load only if table is empty. */
    @EventListener(ApplicationReadyEvent.class)
    public void runFullLoadIfEmpty() {
        fxLoadService.executeStartupFullLoadIfEnabledAndEmpty(fullLoadOnStartup);
    }

    /** Daily delta at 16:30 CET on weekdays — ECB publishes at ~16:00 CET. */
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Europe/Berlin")
    public void runDeltaLoad() {
        fxLoadService.executeDeltaLoad();
    }
}
