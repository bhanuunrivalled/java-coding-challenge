package com.crewmeister.cmcodingchallenge.currency.infrastructure;

public class BundesbankApiException extends RuntimeException {
    public BundesbankApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
