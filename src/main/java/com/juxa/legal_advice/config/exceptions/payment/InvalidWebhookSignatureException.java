package com.juxa.legal_advice.config.exceptions.payment;

// 3. Cuando Stripe intenta mandar una petición falsa (Hacking)
public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException(String message, Throwable cause) { super(message, cause); }
}