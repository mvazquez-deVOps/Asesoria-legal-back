package com.juxa.legal_advice.util.prompt.ontology;

public class DogmaticAreasBlock {
    private DogmaticAreasBlock() {}
    public static final String BLOQUE_IV_MATERIAS = """
            ESPECIFICACIONES DOGMÁTICAS POR RAMA:
            A. DERECHO LABORAL (Bucket: /laboral/, /lft/) - Presunciones irrefragables: Art. 8 LFT (laboralidad), Art. 20 LFT (relación de trabajo), Art. 19 LFT (capacidad, salario, antigüedad).\s
            - Plazos perentorios: 2 años para demandar (Art. 123 fra. XXII LFT - naturales, no hábiles para prescripción).\s
            - Jurisdicción: Federal para colectivos de naturaleza económica; Local para individuales y colectivos no económicos. - Carga probatoria: Invertida a favor del trabajador; el patrón debe probar lo que niega.\s
            - Interés superior del trabajo: Principio rector (Art. 123 CPEUM apartado A).
            B. DERECHO FAMILIAR (Bucket: /familia/, /cnpcyf/, /codigos_locales/[estado]/) - Principio rector: Interés superior del menor (Art. 3 CNPCyF, Art. 4 CPEUM). - Terminología: Usar “guarda y custodia” o “cuidado parental”, evitar “custodia” sola (reforma 2023).\s
            - Alimentos: Porcentajes sobre ingresos netos (30% un hijo, 50% varios hijos, 20% cónyuge) salvo modificación judicial.\s
            - Competencia: Juzgados Familiares locales; excepción: adopciones internacionales (federal).
            C. DERECHO MERCANTIL (Bucket: /comercio/, /ccom/, /ley_toc/)\s
            - Títulos de Crédito: Requisitos de forma estricta (Art. 9 Ley TOC): firma, monto, vencimiento, lugar de pago.\s
            - Procedimiento Ejecutivo: Requisito de título (Art. 1418 C de C); excepciones del librado (Arts. 1428-1430).\s
            - Quiebra/Concurso: Reforma 2023 - ahora procedimiento civil mercantil (no administrativo), concursos universales.\s
            - Días inhábiles: Prohibido vencer obligaciones en domingos (Art. 1933 CCF remite a C de C).
            D. DERECHO PENAL (Bucket: /penal/, /cnpp/, /scjn_penal/)\s
            - PRINCIPIO DE LEGALIDAD ESTRICTA (Art. 14 párrafo 2 CPEUM): Nullum crimen, nulla poena sine lege.\s
            - INTERPRETACIÓN RESTRICTIVA: A favor del imputado (in dubio pro reo).\s
            - PROHIBICIÓN DE ANALOGÍA: Solo analogía in bonam partem (favor reo); prohibida in malam partem. - ESTRUCTURA DEL DELITO: Verificar siempre tipicidad, antijuridicidad (causas de justificación), culpabilidad (dolo/culpa), y punibilidad.\s
            - Sistema Acusatorio: Etapas (investigación, intermedia, juicio oral, ejecución), principio de contradicción y publicidad.
            E. AMPARO Y CONSTITUCIONAL (Bucket: /amparo/, /scjn_const/)\s
            - Control difuso: Cualquier juzgador puede declarar inaplicabilidad de norma por inconstitucionalidad (Art. 195 CPEUM).\s
            - Control concentrado: Acciones de inconstitucionalidad ante SCJN.\s
            - Triple certeza: Certeza del acto reclamado, del perjuicio, y de la ilegalidad.\s
            - Legitimación: Actoridad (quien alega la violación) y agencia (representación).
            F. DERECHO CIVIL GENERAL (Bucket: /civil/, /ccfed/, /cc_estatales/)\s
            - Fuentes de las obligaciones: Contrato, cuasicontrato, delito, cuasidelito, ley.\s
            - Teoría del acto jurídico: Requisitos (capacidad, consentimiento, objeto lícito, causa lícita, forma).\s
            - Prescripción: 10 años obligaciones civiles (Art. 1949 CCF); 2 años alimentos (Art. 412 CCF); interrumpe por demanda o reconocimiento.
            
            """;
}
