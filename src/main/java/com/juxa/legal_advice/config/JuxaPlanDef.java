package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlanDef {

    // ESTRUCTURA:
    // (dbName, maxTokens, maxMonthlyInteractions, aiModel,
    //  miniAppsAccess, appsAccess, docsAccess, constructorAccess,
    //  canUploadAudio, canUploadVideo, hasFullHistory,
    //  HERRAMIENTAS...)

    FREE("FREE", 50000, 3, "gemini-1.5-flash",
            // miniAppsAccess: Solo Redactor, pero permite el resto con tokens
            Access.ONLY_REDACTOR,
            // appsAccess: Gasta tokens (Solo magistrado)
            Access.TOKEN_BASED,
            // docsAccess: Gasta tokens
            Access.TOKEN_BASED,
            // constructorAccess: SIN ACCESO
            Access.LOCKED,

            // Capacidades técnicas
            false, false, false, // Ni audio, ni video, ni historial

            // Herramientas específicas (Booleanos)
            false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false, false
    ,true),

    ESTUDIANTES("estudiantes", 250000, 150, "gemini-2.5-flash",
            Access.EXAM_AND_GUIDE, Access.TOKEN_BASED, Access.TOKEN_BASED, Access.LOCKED,
            false, false, false, // Audio, Video, Historia
            true, true, false, false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, true),

    JUXA_GO("juxa_go", 500000, 240, "gemini-2.5-flash",
            Access.UNLIMITED, Access.TOKEN_BASED, Access.TOKEN_BASED, Access.TOKEN_BASED,
            true, false, true, // Audio, Video, Historia
            true, true, true, true, true,
            true, true, true,
            true, true, true, true,
            true, true, true, true,
            false, true),

    ESENCIAL("esencial_junior", 1000000, 300, "gemini-2.5-flash",
            Access.UNLIMITED, Access.TOKEN_BASED, Access.UNLIMITED, Access.LOCKED,
            true, false, true, // Audio, Video, Historia
            true, true, true, true, true,
            true, true, true,
            true, true, true, true,
            true, true, true, true, false, true),

    PRO("intermedio_pro", 2000000, 810, "gemini-2.5-pro",
            Access.UNLIMITED, Access.UNLIMITED, Access.UNLIMITED, Access.TOKEN_BASED,
            true, false, true, // Audio, Video, Historia
            true, true, true, true, true, true, true,
            true, true, true, true,
            true, true, true, true,
            true, false, true),

    ELITE("premium_elite", -1, 1200, "gemini-2.5-pro",
            Access.UNLIMITED, Access.UNLIMITED, Access.UNLIMITED, Access.UNLIMITED,
            true, true, true, // Audio, Video, Historia
            true, true, true, true, true,
            true, true, true,
            true, true, true, true,

            true, true, true, true,
            false, true);

    @Getter
    public enum Access {
        UNLIMITED(true, false, "Acceso total"),
        TOKEN_BASED(true, true, "Consumo de Bolsita"),
        ONLY_REDACTOR(true, false, "Solo redactor de hechos"),
        EXAM_AND_GUIDE(true, false, "Solo Examen y Guía"),
        LOCKED(false, false, "Módulo no incluido en tu plan");

        private final boolean canEnter;
        private final boolean requiresTokens;
        private final String label;

        Access(boolean canEnter, boolean requiresTokens, String label) {
            this.canEnter = canEnter;
            this.requiresTokens = requiresTokens;
            this.label = label;
        }
    }

    // CAMPOS DE CONFIGURACIÓN
    private final String dbName;
    private final int maxTokens;
    private final int maxMonthlyInteractions;
    private final String aiModel;

    // CAMPOS DE ACCESO POR CATEGORÍA
    private final Access miniAppsAccess;
    private final Access appsAccess;
    private final Access docsAccess;
    private final Access constructorAccess;

    // CAPACIDADES TÉCNICAS
    private final boolean canUploadAudio;
    private final boolean canUploadVideo;
    private final boolean hasFullHistory;

    // BOOLEANOS ESPECÍFICOS DE HERRAMIENTAS
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
    private final boolean canUseConstructor;
    private final boolean canUseRedactor;

    JuxaPlanDef(String dbName, int maxTokens, int maxMonthlyInteractions, String aiModel,
                Access miniAppsAccess, Access appsAccess, Access docsAccess, Access constructorAccess,
                boolean canUploadAudio, boolean canUploadVideo, boolean hasFullHistory,
                boolean canUseExam, boolean canUseGuide, boolean canUseEvidenceValidator, boolean canUseTipicidad,
                boolean canUseMedidasCautelares, boolean canUseTraductorDocumentosLegales, boolean canUseGeneradorInterrogatorios,
                boolean canUseRevisorInterrogatorios, boolean canUsePdfMerger, boolean canUseZipManager,
                boolean canUseFileCompressor, boolean canUseCollectionKpi, boolean canUsePagareGenerator,
                boolean canUseClauseGenerator, boolean canUseCollectionLetter, boolean canUseCartaPoder, boolean canUseConstructor,
                boolean canUseRedactor
                ) {

        this.dbName = dbName;
        this.maxTokens = maxTokens;
        this.maxMonthlyInteractions = maxMonthlyInteractions;
        this.aiModel = aiModel;
        this.miniAppsAccess = miniAppsAccess;
        this.appsAccess = appsAccess;
        this.docsAccess = docsAccess;
        this.constructorAccess = constructorAccess;
        this.canUploadAudio = canUploadAudio;
        this.canUploadVideo = canUploadVideo;
        this.hasFullHistory = hasFullHistory;
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
        this.canUseConstructor = canUseConstructor;
        this.canUseRedactor = canUseRedactor;
    }

    public static JuxaPlanDef fromString(String planName) {
        if (planName == null) return FREE;
        for (JuxaPlanDef plan : values()) {
            if (plan.getDbName().equalsIgnoreCase(planName)) return plan;
        }
        return FREE;
    }

    public Access getAccessForModule(String module) {
        if (module == null) return Access.LOCKED;
        return switch (module.toUpperCase()) {
            case "CHAT" -> Access.TOKEN_BASED;
            case "MINI_APPS" -> this.miniAppsAccess;
            case "APPS" -> this.appsAccess;
            case "DOCS" -> this.docsAccess;
            case "CONSTRUCTOR" -> this.constructorAccess;
            default -> Access.LOCKED;
        };
    }

    public boolean isToolAllowed(String toolName) {
        if (toolName == null || toolName.trim().isEmpty()) return false;
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
            case "redactor-hechos" -> this.canUseRedactor;
            default -> false;
        };
    }
}
