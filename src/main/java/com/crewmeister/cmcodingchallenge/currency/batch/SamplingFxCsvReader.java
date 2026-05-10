package com.crewmeister.cmcodingchallenge.currency.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.file.FlatFileItemReader;

import java.util.Random;

/**
 * Wraps a FlatFileItemReader and randomly samples up to {@code sampleSize}
 * rows.
 * Used in dev profile to test the real Bundesbank API format without loading
 * all history.
 * Each sampled row is logged at DEBUG level for easy inspection.
 */
public class SamplingFxCsvReader implements ItemStreamReader<FxCsvRateRow> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SamplingFxCsvReader.class);

    private final FlatFileItemReader<FxCsvRateRow> delegate;
    private final int sampleSize;
    private final Random random = new Random();

    private int sampledCount = 0;

    public SamplingFxCsvReader(FlatFileItemReader<FxCsvRateRow> delegate, int sampleSize) {
        this.delegate = delegate;
        this.sampleSize = sampleSize;
        LOGGER.info("Dev mode: sampling {} random rows from Bundesbank CSV", sampleSize);
    }

    @Override
    public FxCsvRateRow read() throws Exception {
        if (sampledCount >= sampleSize) {
            return null;
        }

        FxCsvRateRow row;
        while ((row = delegate.read()) != null) {
            if (random.nextBoolean()) {
                sampledCount++;
                LOGGER.debug("Sampled row [{}/{}]: currency={}, date={}, value={}",
                        sampledCount, sampleSize,
                        row.currency(), row.timePeriod(), row.obsValue());
                return row;
            }
        }
        return null;
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        delegate.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegate.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        delegate.close();
    }
}
