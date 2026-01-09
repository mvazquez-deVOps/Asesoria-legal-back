package com.juxa.legal_advice.config;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.transaction.CannotCreateTransactionException;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CannotCreateTransactionException.class)
    public ResponseEntity<?> handleDatabaseConnectionError(CannotCreateTransactionException ex) {
        // Este es el error "Could not open JPA EntityManager"
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "Servicio Temporalmente No Disponible",
                        "message", "Estamos optimizando nuestra base de datos legal. Por favor, intenta de nuevo en unos minutos."
                ));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<?> handleRuntimeError(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        "error", "Error en el Proceso",
                        "message", ex.getMessage()
                ));
    }
}