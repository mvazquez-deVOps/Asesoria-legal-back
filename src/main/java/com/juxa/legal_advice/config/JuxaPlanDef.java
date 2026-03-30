package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // (dbName, maxTokens, aiModel, canUploadAudio, canUploadVideo, hasFullHistory, canUseMiniApps, canUseGenerator, canUseProxy, canUseEducational, canUseAnalysis, canUseSustento, canUseSemantic, canUseMagic, 16 Herramientas extras...)

    FREE("FREE", 50000, "gemini-2.5-flash", false, false, false, false, false, false, false, false, false, false, false,
            true, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),

    ESTUDIANTES("estudiantes", 250000, "gemini-2.5-flash", false, false, false, false, false, false, true, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false),

    JUXA_GO("juxa_go", 7003, "gemini-2.5-flash", false, false, false, true, true, true, true, false, false, false, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),

    ESENCIAL("esencial_junior", 1000000, "gemini-2.5-flash", false, false, true, true, true, true, true, true, false, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),

    PRO("intermedio_pro", 2000000, "gemini-2.5-pro", true, false, true, true, true, true, true, true, true, false, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true),

    ELITE("premium_elite", -1, "gemini-2.5-pro", true, true, true, true, true, true, true, true, true, true, true,
            true, true, true, true, true, true, true, true, true, true, true, true, true, true, true, true);

    private final String dbName;
    private final int maxTokens;
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

    // --- Campos para miniApps ---
    private final boolean canUseExam;
    private final boolean canUseGuide;
    private final boolean canUseEvidenceValidator;
    private final boolean canUseTipicidad;
    private final boolean canUseMedidasCautelares;
    private final boolean canUseTraductorDocumentosLegales;
    private final boolean canUseGeneradorInterrogatorios;
    private final boolean canUseRevisorInterrogatorios;
    private final boolean canUsePdfMerger;
    private final boolean canUseZipManager;
    private final boolean canUseFileCompressor;
    private final boolean canUseCollectionKpi;
    private final boolean canUsePagareGenerator;
    private final boolean canUseClauseGenerator;
    private final boolean canUseCollectionLetter;
    private final boolean canUseCartaPoder;

    JuxaPlanDef(String dbName, int maxTokens, String aiModel, boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory,
                boolean canUseMiniApps, boolean canUseGenerator, boolean canUseProxy, boolean canUseEducational, boolean canUseAnalysis,
                boolean canUseSustento, boolean canUseSemantic, boolean canUseMagic,
                boolean canUseExam, boolean canUseGuide, boolean canUseEvidenceValidator, boolean canUseTipicidad,
                boolean canUseMedidasCautelares, boolean canUseTraductorDocumentosLegales, boolean canUseGeneradorInterrogatorios,
                boolean canUseRevisorInterrogatorios, boolean canUsePdfMerger, boolean canUseZipManager, boolean canUseFileCompressor,
                boolean canUseCollectionKpi, boolean canUsePagareGenerator, boolean canUseClauseGenerator,
                boolean canUseCollectionLetter, boolean canUseCartaPoder) {

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

        this.canUseExam = canUseExam;
        this.canUseGuide = canUseGuide;
        this.canUseEvidenceValidator = canUseEvidenceValidator;
        this.canUseTipicidad = canUseTipicidad;
        this.canUseMedidasCautelares = canUseMedidasCautelares;
        this.canUseTraductorDocumentosLegales = canUseTraductorDocumentosLegales;
        this.canUseGeneradorInterrogatorios = canUseGeneradorInterrogatorios;
        this.canUseRevisorInterrogatorios = canUseRevisorInterrogatorios;
        this.canUsePdfMerger = canUsePdfMerger;
        this.canUseZipManager = canUseZipManager;
        this.canUseFileCompressor = canUseFileCompressor;
        this.canUseCollectionKpi = canUseCollectionKpi;
        this.canUsePagareGenerator = canUsePagareGenerator;
        this.canUseClauseGenerator = canUseClauseGenerator;
        this.canUseCollectionLetter = canUseCollectionLetter;
        this.canUseCartaPoder = canUseCartaPoder;
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

    // Método para validar dinámicamente según el nombre recibido del frontend
    public boolean isToolAllowed(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) {
            return false;
        }

        return switch (toolName.toLowerCase()) {
            case "exam" -> this.canUseExam;
            case "guide" -> this.canUseGuide;
            case "evidence-validator" -> this.canUseEvidenceValidator;
            case "tipicidad" -> this.canUseTipicidad;
            case "medidas-cautelares" -> this.canUseMedidasCautelares;
            case "traductor-documentos-legales" -> this.canUseTraductorDocumentosLegales;
            case "generador-interrogatorios" -> this.canUseGeneradorInterrogatorios;
            case "revisor-interrogatorios" -> this.canUseRevisorInterrogatorios;
            case "pdf-merger" -> this.canUsePdfMerger;
            case "zip-manager" -> this.canUseZipManager;
            case "file-compressor" -> this.canUseFileCompressor;
            case "collection-kpi" -> this.canUseCollectionKpi;
            case "pagare-generator" -> this.canUsePagareGenerator;
            case "clause-generator" -> this.canUseClauseGenerator;
            case "collection-letter" -> this.canUseCollectionLetter;
            case "carta-poder" -> this.canUseCartaPoder;
            default -> false; // Si envían algo que no existe, se bloquea por defecto
        };
    }
}