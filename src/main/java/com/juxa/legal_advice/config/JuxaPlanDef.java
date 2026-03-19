package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // NOMBRES EXACTOS que guardas en tu tabla users (columna subscription_plan)
    // El -1 lo usaremos para representar "Ilimitado"

    FREE("FREE", -1, 0, "gemini-1.5-flash", false),
    ESTUDIANTES("estudiantes", 20, 1, "gemini-1.5-flash", false),
    JUXA_GO("juxa_go", 20, 3, "gemini-1.5-flash", true),
    JUNIOR("junior", 30, 5, "gemini-1.5-flash", true),
    PRO("pro", 50, 10, "gemini-1.5-pro", true),
    ELITE("elite", -1, -1, "gemini-3.0-pro", true);

    private final String dbName;
    private final int maxQueriesPerDay;  // Consultas por día
    private final int maxFilesPerDay;    // Archivos por día
    private final String aiModel;        // Modelo a utilizar
    private final boolean canUploadAudio;// Texto + Audio (Pro/Elite)

    JuxaPlanDef(String dbName, int maxQueriesPerDay, int maxFilesPerDay, String aiModel, boolean canUploadAudio) {
        this.dbName = dbName;
        this.maxQueriesPerDay = maxQueriesPerDay;
        this.maxFilesPerDay = maxFilesPerDay;
        this.aiModel = aiModel;
        this.canUploadAudio = canUploadAudio;
    }

    // Método de utilidad para buscar el plan del usuario basado en el String de la BD
    public static JuxaPlanDef fromString(String planName) {
        if (planName == null) return FREE; // Fallback por seguridad

        for (JuxaPlanDef plan : values()) {
            if (plan.getDbName().equalsIgnoreCase(planName)) {
                return plan;
            }
        }
        return FREE; // Si no lo encuentra, asume Free
    }
}