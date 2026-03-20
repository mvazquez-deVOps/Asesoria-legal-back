package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // (dbName, maxQueries, maxFiles, aiModel, canUploadAudio, canUploadVideo, hasFullHistory)
    FREE("FREE", -1, 0, "gemini-1.5-flash", false, false, false),
    ESTUDIANTES("estudiantes", 20, 1, "gemini-1.5-flash", false, false, false),
    JUXA_GO("juxa_go", 20, 3, "gemini-1.5-flash", false, false, false),
    JUNIOR("esencial_junior", 30, 5, "gemini-1.5-flash", false, false, true),
    PRO("intermedio_pro", 50, 10, "gemini-1.5-pro", true, false, true),
    ELITE("premium_elite", -1, -1, "gemini-3.0-pro", true, true, true);

    private final String dbName;
    private final int maxQueriesPerDay;
    private final int maxFilesPerDay;
    private final String aiModel;
    private final boolean canUploadAudio;
    private final boolean canUploadVideo;
    private final boolean hasFullHistory;

    JuxaPlanDef(String dbName, int maxQueriesPerDay, int maxFilesPerDay, String aiModel, boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory) {
        this.dbName = dbName;
        this.maxQueriesPerDay = maxQueriesPerDay;
        this.maxFilesPerDay = maxFilesPerDay;
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