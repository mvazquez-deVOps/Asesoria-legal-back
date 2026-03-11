package com.juxa.legal_advice.util.prompt.ontology;

public class CitationBlock {
    private CitationBlock() {}
    public static final String BLOQUE_VII_CITATION = """
            PROTOCÓLO DE CITACIÓN ACADÉMICA-JURÍDICA:
            A. CITACIÓN DE NORMAS:
               Formato estricto: "Art. [Número], [Nombre Completo del Código/Ley], publicado en el DOF el [fecha], última reforma [fecha bucket], Título [X], Capítulo [Y]."
               Ejemplo correcto: "Art. 1917, Código Civil Federal, publicado en el DOF el 31-05-1928, última reforma 15-06-2023, Título Décimo Tercero, Capítulo I, De la Prescripción."
            
               PROHIBIDO: Abreviaturas inestables ("C.C.F.", "Cod. Civ. Fed"). Usar: Código Civil Federal (CCF).
            
            B. CITACIÓN DE JURISPRUDENCIA:
               Estructura: [Clase] [Número]/[Año], [Nombre de la Tesis], [Véase] o [Contradice].
               Ejemplo: "Jurisprudencia 1a. CCLXV/2016 (10a.), Contradición de laudos, relativo a la irretroactividad de laudos..."
               Metadatos obligatorios: Época, Sala, Instancia, Tesis aislada o de jurisprudencia.
            
            C. CITACIÓN DE DOCTRINA (solo si existe en /doctrina/):
               Formato: [Autor], [Título], [Editorial], [Edición], [Año], [Página].
               Verificación: Confirmar que el PDF está indexado en el bucket antes de citar.
            
            D. TRAZABILIDAD DE FUENTES:
               Cada afirmación de derecho positivo debe ser trazable a:
               - Ruta del bucket: ej. gs://[bucket]/codigos/ccf/art_1917.txt
               - Hash del documento (si está disponible en metadatos)
               - Fecha de indexación: para control de versiones
            
            E. CONTROL DE VIGENCIA TEMPORAL:
               Flag en cada cita: [VIGENTE], [DEROGADO_PARA_ANALISIS_HISTORICO], [REFORMADO_POSTERIOR].
               Si el usuario pregunta por derecho aplicable a hechos pasados: Especificar la versión vigente al momento del hecho generador (irretroactividad).
           
            """;
}
