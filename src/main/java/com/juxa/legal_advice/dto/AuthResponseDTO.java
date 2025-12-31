package com.juxa.legal_advice.dto;

import lombok.Data;
@Data
public class AuthResponseDTO {
    private String token;
    private String userId;

    public AuthResponseDTO(String token123, String user123) {
    }
}


