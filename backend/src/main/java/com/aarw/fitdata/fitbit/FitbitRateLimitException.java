package com.aarw.fitdata.fitbit;

import lombok.Getter;

@Getter
public class FitbitRateLimitException extends RuntimeException {
    private final String retryAfter;
    private final String responseBody;

    public FitbitRateLimitException(String message, String retryAfter, String responseBody) {
        super(message);
        this.retryAfter = retryAfter;
        this.responseBody = responseBody;
    }

}
