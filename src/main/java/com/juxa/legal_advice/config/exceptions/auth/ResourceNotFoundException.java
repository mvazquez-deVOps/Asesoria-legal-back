package com.juxa.legal_advice.config.exceptions.auth;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) { super(message); }
}