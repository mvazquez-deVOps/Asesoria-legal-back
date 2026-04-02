package com.juxa.legal_advice.config;
import java.util.Arrays;

public enum TokenPackageDef {
    BOLSA_RESPALDO("bolsa_respaldo", "Bolsa Respaldo", 400000L),
    BOLSA_RESOLUCION("bolsa_resolucion", "Bolsa Resolución", 900000L),
    BOLSA_ESTRATEGA("bolsa_estratega", "Bolsa Estratega", 1500000L),
    BOLSA_LITIGIO_ACTIVO("bolsa_litigio_activo", "Bolsa Litigio Activo", 3200000L),
    BOLSA_EXPEDIENTES("bolsa_expedientes", "Bolsa Expedientes", 6000000L),
    BOLSA_OPERACION_TOTAL("bolsa_operacion_total", "Bolsa Operación Total", 8500000L),
    BOLSA_INFRAESTRUCTURA("bolsa_infraestructura", "Bolsa Infraestructura", 12000000L);

    private final String dbName;       // El nombre con el que lo guardarás en tu BD (PlanEntity)
    private final String displayName;  // El nombre comercial
    private final Long tokenAmount;    // La cantidad real de tokens que otorga

    TokenPackageDef(String dbName, String displayName, Long tokenAmount) {
        this.dbName = dbName;
        this.displayName = displayName;
        this.tokenAmount = tokenAmount;
    }

    public String getDbName() { return dbName; }
    public String getDisplayName() { return displayName; }
    public Long getTokenAmount() { return tokenAmount; }

    // Método para buscar el paquete desde el string que manda el Frontend
    public static TokenPackageDef fromDbName(String dbName) {
        return Arrays.stream(values())
                .filter(pack -> pack.dbName.equalsIgnoreCase(dbName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Paquete de tokens inválido: " + dbName));
    }
}