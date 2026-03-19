package com.juxa.legal_advice.config;

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
}