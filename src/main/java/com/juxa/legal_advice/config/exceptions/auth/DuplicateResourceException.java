package com.juxa.legal_advice.config.exceptions.auth;

public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) { super(message); }
}