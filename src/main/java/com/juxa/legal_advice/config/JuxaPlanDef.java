package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // (dbName, maxTokens, aiModel, canUploadAudio, canUploadVideo, hasFullHistory)
    FREE("FREE", 50000, "gemini-1.5-flash", false, false, false), // 50k tokens (aprox. 30 interacciones simples)
    ESTUDIANTES("estudiantes", 250000, "gemini-1.5-flash", false, false, false),
    JUXA_GO("juxa_go", /*500000*/ 7003, "gemini-1.5-flash", false, false, false),
    JUNIOR("esencial_junior", 1000000, "gemini-1.5-flash", false, false, true),
    PRO("intermedio_pro", 2000000, "gemini-1.5-pro", true, false, true),
    ELITE("premium_elite", -1, "gemini-1.5-pro", true, true, true); // -1 = Ilimitado (Uso justo)

    private final String dbName;
    private final int maxTokens; // Ahora el límite es por Tokens
    private final String aiModel;
    private final boolean canUploadAudio;
    private final boolean canUploadVideo;
    private final boolean hasFullHistory;

    JuxaPlanDef(String dbName, int maxTokens, String aiModel, boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory) {
        this.dbName = dbName;
        this.maxTokens = maxTokens;
        this.aiModel = aiModel;
        this.canUploadAudio = canUploadAudio;
        this.canUploadVideo = canUploadVideo;
        this.hasFullHistory = hasFullHistory;
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