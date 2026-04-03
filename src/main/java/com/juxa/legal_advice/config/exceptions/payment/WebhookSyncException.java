package com.juxa.legal_advice.config.exceptions.payment;

// 2. Cuando el webhook busca datos en nuestra BD y no existen
public class WebhookSyncException extends RuntimeException {
  public WebhookSyncException(String message) { super(message); }
}