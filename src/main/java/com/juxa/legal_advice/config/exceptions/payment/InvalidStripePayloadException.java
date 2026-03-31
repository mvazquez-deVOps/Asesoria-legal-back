package com.juxa.legal_advice.config.exceptions.payment;

// 1. Cuando Stripe manda algo que no entendemos o no cuadra con nuestro código
public class InvalidStripePayloadException extends RuntimeException {
    public InvalidStripePayloadException(String message) { super(message); }
}