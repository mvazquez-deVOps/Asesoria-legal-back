package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    FREE("FREE", -1, 0, "gemini-1.5-flash", false),
    ESTUDIANTES("estudiantes", 20, 1, "gemini-1.5-flash", false),
    JUXA_GO("juxa_go", 20, 3, "gemini-1.5-flash", true),

    // Nombres actualizados para coincidir con tu MySQL
    JUNIOR("esencial_junior", 30, 5, "gemini-1.5-flash", true),
    PRO("intermedio_pro", 50, 10, "gemini-1.5-pro", true),
    ELITE("premium_elite", -1, -1, "gemini-3.0-pro", true);

    private final String dbName;
    private final int maxQueriesPerDay;
    private final int maxFilesPerDay;
    private final String aiModel;
    private final boolean canUploadAudio;

    JuxaPlanDef(String dbName, int maxQueriesPerDay, int maxFilesPerDay, String aiModel, boolean canUploadAudio) {
        this.dbName = dbName;
        this.maxQueriesPerDay = maxQueriesPerDay;
        this.maxFilesPerDay = maxFilesPerDay;
        this.aiModel = aiModel;
        this.canUploadAudio = canUploadAudio;
    }

    public static JuxaPlanDef fromString(String planName) {
        if (planName == null) return FREE;

        for (JuxaPlanDef plan : values()) {
            if (plan.getDbName().equalsIgnoreCase(planName)) {
                return plan;
            }
        }
        return FREE;
    }
}