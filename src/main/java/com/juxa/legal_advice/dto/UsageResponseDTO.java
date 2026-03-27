package com.juxa.legal_advice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsageResponseDTO {
    private String planName;

    // --- LÍMITES REALES (NUEVO SISTEMA) ---
    private Integer tokensUsed;
    private Integer tokensLimit;
    private Integer extraTokens;

    // --- ESTADÍSTICAS (Informativo para el usuario) ---
    private Integer queriesUsed;
    private Integer filesUsed;

    // --- CAPACIDADES DEL MODELO ---
    private String aiModel;

    // Banderas de control para el Frontend (UI/UX)
    private boolean canMakeMoreQueries; // Ahora dependerá de los tokens
    private boolean canUploadMoreFiles; // Puedes dejarlo en true siempre si ya no hay límite de archivos
    private boolean canUploadAudio;
    private boolean canUploadVideo;
    private boolean hasFullHistory;
    private boolean canUseMiniApps;
    private boolean canUseGenerator;
    private boolean canUseProxy;
    private boolean canUseEducational;
}