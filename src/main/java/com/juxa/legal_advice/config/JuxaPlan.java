package com.juxa.legal_advice.config;

import lombok.Getter;

@Getter
public enum JuxaPlan {

    // 1. Definición de los planes con sus valores hardcodeados
    // (nombre_metadata, precio_mxn, archivos_dia, consultas_dia, sesion_pers, retomar_conv, vertex_search, modelo_respuestas)
    ESTUDIANTES("estudiantes", 129.0, 5, 20, false, false, false, "Gemini Flash"),
    JUXA_GO("juxa_go", 249.0, 15, 50, false, true, false, "Gemini Flash"),
    ESENCIAL_JUNIOR("esencial_junior", 549.0, 30, 150, true, true, true, "Gemini Pro"),
    INTERMEDIO_PRO("intermedio_pro", 999.0, 100, 500, true, true, true, "Gemini Pro"),
    PREMIUM_ELITE("premium_elite", 2999.0, 500, 2000, true, true, true, "Gemini Ultra");

    // ==========================================
    // 2. Variables de configuración (Las que me pediste)
    // ==========================================

    // Identificador para cruzar con Stripe (basado en tu CSV)
    private final String stripeMetadataName;
    private final double precioMxn;

    // Límites operativos (Los que van a la Base de Datos para validación diaria)
    private final int archivosPorDia;
    private final int consultasPorDia;
    private final boolean sesionPersonalizada;
    private final boolean retomarConversacion;

    // Características del sistema
    private final boolean vertexSearchHabilitado;
    private final String modeloRespuestas;

    // Características Generales (Comunes en todos los planes o con texto fijo)
    // Como estas aplican para todos, las podemos definir como getters fijos o variables estáticas
    private final boolean proteccionContraEntrenamiento = true;
    private final boolean proteccionDatosPersonales = true;
    private final boolean transparenciaFuentes = true;
    private final boolean consultaMisInteracciones = true;

    // Constructor del Enum
    JuxaPlan(String stripeMetadataName, double precioMxn, int archivosPorDia, int consultasPorDia,
             boolean sesionPersonalizada, boolean retomarConversacion,
             boolean vertexSearchHabilitado, String modeloRespuestas) {

        this.stripeMetadataName = stripeMetadataName;
        this.precioMxn = precioMxn;
        this.archivosPorDia = archivosPorDia;
        this.consultasPorDia = consultasPorDia;
        this.sesionPersonalizada = sesionPersonalizada;
        this.retomarConversacion = retomarConversacion;
        this.vertexSearchHabilitado = vertexSearchHabilitado;
        this.modeloRespuestas = modeloRespuestas;
    }

    // ==========================================
    // 3. Métodos útiles para tu lógica de negocio
    // ==========================================

    /**
     * Busca la configuración del plan a partir del metadata que devuelve Stripe
     */
    public static JuxaPlan fromStripeMetadata(String metadataName) {
        for (JuxaPlan plan : JuxaPlan.values()) {
            if (plan.getStripeMetadataName().equalsIgnoreCase(metadataName)) {
                return plan;
            }
        }
        throw new IllegalArgumentException("Plan no encontrado para el metadata: " + metadataName);
    }
}