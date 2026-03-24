package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // (dbName, maxTokens, maxOcrPages, aiModel, canUploadAudio, canUploadVideo, hasFullHistory)
    FREE("FREE", 50000,     5, "gemini-1.5-flash", false, false, false),
    ESTUDIANTES("estudiantes", 250000, 20, "gemini-1.5-flash", false, false, false),
    JUXA_GO("juxa_go", 7003, 50, "gemini-1.5-flash", false, false, false),
    JUNIOR("esencial_junior", 1000000, 100, "gemini-1.5-flash", false, false, true),
    PRO("intermedio_pro", 2000000, 300, "gemini-1.5-pro", true, false, true),
    ELITE("premium_elite", -1, -1, "gemini-1.5-pro", true, true, true); // -1 = Ilimitado

    private final String dbName;
    private final int maxTokens;
    private final int maxOcrPages; // NUEVO CAMPO
    private final String aiModel;
    private final boolean canUploadAudio;
    private final boolean canUploadVideo;
    private final boolean hasFullHistory;

    JuxaPlanDef(String dbName, int maxTokens, int maxOcrPages, String aiModel, boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory) {
        this.dbName = dbName;
        this.maxTokens = maxTokens;
        this.maxOcrPages = maxOcrPages;
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