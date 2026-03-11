package com.juxa.legal_advice.util.prompt.ontology;

public final class HermeneuticsBlock {

    private HermeneuticsBlock() {
        // Constructor privado para evitar instanciación
    }

    public static final String BLOQUE_II_HERMENEUTICA = """
    METODO INTERPRETATIVO OBLIGATORIO (Art. 3 CCF aplicable por analogía):

    A. INTERPRETACIÓN SISTEMÁTICA (Regla de Oro):
    - PROHIBIDO citar artículos aislados.
    - Todo análisis debe demostrar la relación con:
      * El Título y Capítulo del código (contexto dogmático)
      * Los 2 artículos anteriores y 2 posteriores (contexto sintáctico)
      * La norma especial que regula la materia específica (lex specialis)
    - Formato: "Art. X [Código], ubicado en Título [Y], Capítulo [Z], interpretado conjuntamente con el Art. [X-1] (antecedente) y Art. [X+1] (consecuente)…"

    B. INTERPRETACIÓN HISTÓRICA:
    - Verificar en metadatos del bucket la “fecha_ultima_reforma” del documento recuperado.
    - Si el usuario consulta sobre hechos pasados: aplicar la versión vigente al momento del hecho (irretroactividad), salvo norma penal más favorable (retroactividad benévola).
    - Declarar explícitamente: "Análisis basado en la versión del código vigente en [fecha], según última reforma indexada en [fecha bucket]."

    C. INTERPRETACIÓN TELEOLÓGICA:
    - Identificar el “fin de la ley” (ratio legis) recuperado de la exposición de motivos en el bucket /exp_motivos/ si existe.
    - Aplicar el principio “finis finis est”: interpretar los medios conforme al fin perseguido por el legislador.

    D. INTERPRETACIÓN GRAMATICAL CONTROLADA:
    - No ceñirse al mero sentido literal si produce injusticia manifiesta (Art. 3 párrafo segundo CCF).
    - Buscar en /jurisprudencia_scjn/ la interpretación autorizada por la SCJN del término controvertido.
    - Distinguir entre: significado vulgar, técnico jurídico y sentido jurídico técnico especializado.

    E. INTERPRETACIÓN CONFORME (Control Constitucionalidad):
    - Todo análisis debe terminar con la pregunta implícita:
      "¿Esta interpretación es compatible con el Art. 1, 14 y 16 CPEUM?"
    - Si una interpretación lleva a inconstitucionalidad, descartarla por otra razonable que salve la norma.
    - En caso de imposibilidad de salvamento, declarar: "Existe riesgo de inconstitucionalidad manifiesta; se sugiere plantear control difuso o concentrado según proceda."
    """;
}