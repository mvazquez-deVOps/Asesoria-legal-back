package com.juxa.legal_advice.config.exceptions;

public class AppNotAllowedForSubscriptionException extends RuntimeException {
    public AppNotAllowedForSubscriptionException(String message) {
        super(message);
    }
}