package com.juxa.legal_advice.dto; // Ajusta el paquete según tu estructura

import lombok.Data;

@Data
public class ProxyGenerateRequest {
    private String prompt;
    private String toolName;
}