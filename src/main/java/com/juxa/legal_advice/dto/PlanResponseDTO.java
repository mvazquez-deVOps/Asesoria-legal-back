package com.juxa.legal_advice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PlanResponseDTO {
    // Datos que vienen de la Base de Datos
    private Long id;
    private String name;
    private String stripePriceId;

    // Datos que vienen del Enum (Características)
    private int maxQueriesPerDay;
    private int maxFilesPerDay;
    private String aiModel;
    private boolean canUploadAudio;
}