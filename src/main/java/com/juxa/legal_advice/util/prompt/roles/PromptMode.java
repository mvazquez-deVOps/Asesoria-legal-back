package com.juxa.legal_advice.util.prompt.roles;

public enum PromptMode {
    CHAT("Consulta/Análisis jurídico"),
    REDACCION("Generación Documental"),
    CONTRATO("Contrato Inteligente"),
    JURISPRUDENCIAL("Análisis de Tesis y Precedentes");

    private final String description;

    PromptMode(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
