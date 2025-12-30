package com.juxa.legal_advice.model;
import lombok.*;

import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiagnosisResponse {
    // Resumen legal generado por la IA
    private String summary;

    // Pasos sugeridos para el usuario
    private List<String> steps;

    // Nivel de riesgo o urgencia (ej. Preventivo, Notificado, En Juicio, Emergencia)
    private String riskLevel;

    // Mensaje adicional del sistema o abogado
    private String advisorNote;

    // Identificador del diagnóstico (para trazabilidad)
    private Long diagnosisId;
}


