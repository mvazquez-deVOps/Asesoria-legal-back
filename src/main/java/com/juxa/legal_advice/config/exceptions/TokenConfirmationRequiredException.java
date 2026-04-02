package com.juxa.legal_advice.config.exceptions;

/**
 * Esta excepción le avisa al sistema que el usuario ya no tiene mensajes gratis
 * y necesita confirmar si quiere usar sus tokens de la bolsita.
 */
public class TokenConfirmationRequiredException extends RuntimeException {
    public TokenConfirmationRequiredException(String message) {
        super(message);
    }
}