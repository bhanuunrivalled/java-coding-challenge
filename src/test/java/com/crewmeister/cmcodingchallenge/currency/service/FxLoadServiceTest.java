package com.crewmeister.cmcodingchallenge.currency.service;

import com.crewmeister.cmcodingchallenge.currency.infrastructure.FxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FxLoadServiceTest {

    @Mock
    private FxRateRepository fxRateRepository;
    @Mock
    private JobLauncher jobLauncher;
    @Mock
    private Job fullLoadJob;
    @Mock
    private Job deltaLoadJob;
    @Mock
    private CurrencyQueryService currencyQueryService;
    @Mock
    private JobExecution jobExecution;

    private FxLoadService fxLoadService;

    /** Fixed clock: "today" is always 2026-01-10 */
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 10);
    private static final Clock FIXED_CLOCK = Clock.fixed(
            TODAY.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    @BeforeEach
    void setUp() {
        fxLoadService = new FxLoadService(
                fxRateRepository, jobLauncher, fullLoadJob, deltaLoadJob, currencyQueryService, FIXED_CLOCK);
    }

    @Test
    void should_returnSkippedEmpty_when_noRatesExist() {
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.empty());

        DeltaLoadResult result = fxLoadService.executeDeltaLoad();

        assertEquals(DeltaLoadResult.Status.SKIPPED_EMPTY, result.status());
        assertNull(result.startDate());
    }

    @Test
    void should_returnSkippedUpToDate_when_maxRateDateIsToday() {
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.of(TODAY));

        DeltaLoadResult result = fxLoadService.executeDeltaLoad();

        assertEquals(DeltaLoadResult.Status.SKIPPED_UP_TO_DATE, result.status());
    }

    @Test
    void should_returnSkippedUpToDate_when_maxRateDateIsTomorrow() {
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.of(TODAY.plusDays(1)));

        DeltaLoadResult result = fxLoadService.executeDeltaLoad();

        assertEquals(DeltaLoadResult.Status.SKIPPED_UP_TO_DATE, result.status());
    }

    @Test
    void should_launchJobWithCorrectDates_when_deltaWindowIsValid() throws Exception {
        LocalDate maxRateDate = LocalDate.of(2026, 1, 7);
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.of(maxRateDate));
        when(jobLauncher.run(eq(deltaLoadJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        DeltaLoadResult result = fxLoadService.executeDeltaLoad();

        assertEquals(DeltaLoadResult.Status.SUCCESS, result.status());
        assertEquals(LocalDate.of(2026, 1, 8), result.startDate());
        assertEquals(TODAY, result.endDate());
        verify(currencyQueryService).evictCurrenciesCache();
    }

    @Test
    void should_notLaunchJob_when_databaseIsEmpty() throws Exception {
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.empty());

        fxLoadService.executeDeltaLoad();

        verify(jobLauncher, never()).run(any(), any());
    }

    @Test
    void should_throwException_when_jobFails() throws Exception {
        when(fxRateRepository.findMaxRateDate()).thenReturn(Optional.of(LocalDate.of(2026, 1, 7)));
        when(jobLauncher.run(eq(deltaLoadJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.FAILED);

        assertThrows(RuntimeException.class, () -> fxLoadService.executeDeltaLoad());
    }

    @Test
    void should_notLaunchFullLoad_when_startupLoadDisabled() throws Exception {
        fxLoadService.executeStartupFullLoadIfEnabledAndEmpty(false);

        verify(fxRateRepository, never()).count();
        verify(jobLauncher, never()).run(eq(fullLoadJob), any(JobParameters.class));
    }

    @Test
    void should_notLaunchFullLoad_when_storeAlreadyPopulated() throws Exception {
        when(fxRateRepository.count()).thenReturn(1L);

        fxLoadService.executeStartupFullLoadIfEnabledAndEmpty(true);

        verify(fxRateRepository, times(1)).count();
        verify(jobLauncher, never()).run(eq(fullLoadJob), any(JobParameters.class));
    }

    @Test
    void should_launchFullLoad_when_startupLoadEnabledAndStoreEmpty() throws Exception {
        when(fxRateRepository.count()).thenReturn(0L);
        when(jobLauncher.run(eq(fullLoadJob), any(JobParameters.class))).thenReturn(jobExecution);
        when(jobExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);

        fxLoadService.executeStartupFullLoadIfEnabledAndEmpty(true);

        verify(jobLauncher, times(1)).run(eq(fullLoadJob), any(JobParameters.class));
        verify(currencyQueryService, times(1)).evictCurrenciesCache();
    }
}
