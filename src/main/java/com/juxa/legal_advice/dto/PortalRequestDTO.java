package com.juxa.legal_advice.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PortalRequestDTO {
    // Solicitamos el ID del usuario para buscar su Customer ID en la BD.
    // Nota: Si usas Spring Security, esto también podrías sacarlo del token JWT directamente en el controlador.
    private Long userId;
}