package com.juxa.legal_advice.config;

import com.juxa.legal_advice.config.exceptions.PlanLimitExceededException;
import com.juxa.legal_advice.config.exceptions.UnauthorizedUserException;
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
}