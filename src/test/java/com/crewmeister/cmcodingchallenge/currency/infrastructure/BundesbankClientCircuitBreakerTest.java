package com.crewmeister.cmcodingchallenge.currency.infrastructure;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "resilience4j.circuitbreaker.instances.bundesbankApi.sliding-window-size=3",
        "resilience4j.circuitbreaker.instances.bundesbankApi.failure-rate-threshold=50",
        "resilience4j.circuitbreaker.instances.bundesbankApi.wait-duration-in-open-state=1s",
        "resilience4j.circuitbreaker.instances.bundesbankApi.permitted-number-of-calls-in-half-open-state=1"
})
class BundesbankClientCircuitBreakerTest {

    @Autowired
    private BundesbankClient bundesbankClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Test
    void should_openCircuitBreaker_when_failureThresholdExceeded() {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("bundesbankApi");

        assertEquals(CircuitBreaker.State.CLOSED, cb.getState());

        for (int i = 0; i < 3; i++) {
            assertThrows(BundesbankApiException.class, () -> bundesbankClient.fetchFullLoad());
        }

        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        assertThrows(BundesbankApiException.class, () -> bundesbankClient.fetchFullLoad());
    }

    @Test
    void should_transitionToHalfOpen_when_waitDurationExpires() throws InterruptedException {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("bundesbankApi");

        for (int i = 0; i < 3; i++) {
            assertThrows(BundesbankApiException.class, () -> bundesbankClient.fetchFullLoad());
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());

        Thread.sleep(1200);

        assertThrows(BundesbankApiException.class, () -> bundesbankClient.fetchFullLoad());
        assertEquals(CircuitBreaker.State.OPEN, cb.getState());
    }
}
