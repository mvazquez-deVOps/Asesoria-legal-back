package com.juxa.legal_advice.config;

import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.config.exceptions.UnauthorizedUserException;
import com.juxa.legal_advice.config.exceptions.auth.DuplicateResourceException;
import com.juxa.legal_advice.config.exceptions.auth.InvalidCredentialsException;
import com.juxa.legal_advice.config.exceptions.auth.ResourceNotFoundException;
import com.juxa.legal_advice.config.exceptions.payment.InvalidStripePayloadException;
import com.juxa.legal_advice.config.exceptions.payment.InvalidWebhookSignatureException;
import com.juxa.legal_advice.config.exceptions.payment.WebhookSyncException;
import com.stripe.exception.StripeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<?> handleDatabaseConnectionError(CannotCreateTransactionException ex) {
        log.error("ERROR CRÍTICO DB: ", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Servicio Temporalmente No Disponible",
                        "message", "Estamos optimizando nuestra base de datos legal. Por favor, intenta de nuevo en unos minutos."
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeError(RuntimeException ex) {
        log.error("Excepción capturada en el proceso: ", ex);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Error en el Proceso",
                        "message", ex.getMessage()
                ));
    }

    // Maneja cuando se les acaba la cuota (403 Forbidden)
    @ExceptionHandler(PlanLimitExceededException.class)
    public ResponseEntity<Map<String, String>> handlePlanLimitExceeded(PlanLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of(
                "error", "Límite de Plan Alcanzado",
                "message", ex.getMessage()
        ));
    }

    // Maneja cuando no están logueados o caducó su trial (401 Unauthorized)
    @ExceptionHandler(UnauthorizedUserException.class)
    public ResponseEntity<Map<String, String>> handleUnauthorizedUser(UnauthorizedUserException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Acceso Denegado",
                "message", ex.getMessage()
        ));
    }

    // (Opcional) Un manejador general por si se rompe otra cosa (500 Internal Server Error)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error Interno del Servidor",
                "message", ex.getMessage()
        ));
    }

    // ... tus manejadores actuales (PlanLimitExceededException, etc) ...

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleNotFound(ResourceNotFoundException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                "error", "No Encontrado",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Intento de acceso fallido: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                "error", "Acceso Denegado",
                "message", ex.getMessage()
        ));
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<Map<String, String>> handleDuplicateResource(DuplicateResourceException ex) {
        log.warn("Conflicto de recursos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "error", "Conflicto de Datos",
                "message", ex.getMessage()
        ));
    }

    // 1. Alguien intentó hackear el Webhook o mandar datos corruptos (Stripe no reintentará)
    @ExceptionHandler({InvalidWebhookSignatureException.class, InvalidStripePayloadException.class})
    public ResponseEntity<String> handleStripeSecurityErrors(RuntimeException ex) {
        log.error("Error de seguridad/datos en Webhook de Stripe: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Petición inválida.");
    }

    // 2. Nuestra base de datos falló al sincronizar el pago (Stripe SI reintentará)
    @ExceptionHandler(WebhookSyncException.class)
    public ResponseEntity<String> handleWebhookSyncError(WebhookSyncException ex) {
        log.error("Error de sincronización con BD al procesar Webhook. Stripe reintentará.", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error de sincronización, reintentar más tarde.");
    }

    // 3. Cualquier error general de la API de Stripe (Evitamos fuga de datos)
    @ExceptionHandler(com.stripe.exception.StripeException.class)
    public ResponseEntity<Map<String, String>> handleStripeApiError(StripeException ex) {
        log.error("Fallo externo en la API de Stripe: ", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "error", "Servicio de Pagos No Disponible",
                "message", "Ocurrió un error al comunicar con la pasarela de pagos. Intenta más tarde."
        ));
    }

    // Maneja envíos de datos incorrectos del Frontend (ej. Enums no válidos)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Petición con argumentos inválidos: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                "error", "Petición Inválida",
                "message", ex.getMessage() // Asegúrate de que tus Enums lancen mensajes amigables
        ));
    }
}