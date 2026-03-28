package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // (dbName, maxTokens, aiModel, canUploadAudio, canUploadVideo, hasFullHistory)
    FREE("FREE", 50000, "gemini-2.5-flash", false, false, false, false, false, false, false, false, false, false, false), // 50k tokens (aprox. 30 interacciones simples)
    ESTUDIANTES("estudiantes", 250000, "gemini-2.5-flash", false, false, false, false, false, false, true, false, false, false, false),
    JUXA_GO("juxa_go", /*500000*/ 7003, "gemini-2.5-flash", false, false, false, true, true, true, true, false, false, false, true),
    ESENCIAL("esencial_junior", 1000000, "gemini-2.5-flash", false, false, true, true, true, true, true, true, false, true, true),
    PRO("intermedio_pro", 2000000, "gemini-2.5-pro", true, false, true, true, true, true, true, true, true, false, true),
    ELITE("premium_elite", -1, "gemini-2.5-pro", true, true, true, true, true, true, true, true, true, true, true); // -1 = Ilimitado (Uso justo)

    private final String dbName;
    private final int maxTokens; // Ahora el límite es por Tokens
    private final String aiModel;
    private final boolean canUploadAudio;
    private final boolean canUploadVideo;
    private final boolean hasFullHistory;
    private final boolean canUseMiniApps;
    private final boolean canUseGenerator;
    private final boolean canUseProxy;
    private final boolean canUseEducational;
    private final boolean canUseAnalysis;
    private final boolean canUseSustento;
    private final boolean canUseSemantic;
    private final boolean canUseMagic;




    JuxaPlanDef(String dbName, int maxTokens, String aiModel, boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory, boolean canUseMiniApps, boolean canUseGenerator, boolean canUseProxy, boolean canUseEducational, boolean canUseAnalysis, boolean canUseSustento, boolean canUseSemantic, boolean canUseMagic) {
        this.dbName = dbName;
        this.maxTokens = maxTokens;
        this.aiModel = aiModel;
        this.canUploadAudio = canUploadAudio;
        this.canUploadVideo = canUploadVideo;
        this.hasFullHistory = hasFullHistory;
        this.canUseMiniApps = canUseMiniApps;
        this.canUseGenerator = canUseGenerator;
        this.canUseProxy = canUseProxy;
        this.canUseEducational = canUseEducational;
        this.canUseAnalysis = canUseAnalysis;
        this.canUseSustento = canUseSustento;
        this.canUseSemantic = canUseSemantic;
        this.canUseMagic = canUseMagic;


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