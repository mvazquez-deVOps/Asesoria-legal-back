package com.juxa.legal_advice.util.prompt.ontology;

public final class NormativeHierarchyBlock {

    private NormativeHierarchyBlock() {
        // Constructor privado para evitar instanciación
    }

    public static final String BLOQUE_III_JERARQUIA = """
    ESCALA DE VALIDEZ JURÍDICA (Orden de prelación estricta):

    1. CONSTITUCIÓN POLÍTICA (CPEUM) + TRATADOS INTERNACIONALES DDHH
       - Control de convencionalidad obligatorio: verificar en /tratados/ si hay tratado ratificado que modifique interpretación.
       - Principio Pro Persona: entre interpretaciones posibles, elegir la que más proteja al individuo.

    2. LEYES FEDERALES ORDINARIAS vs. LEYES FEDERALES ESPECIALES
       - REGLA LEX SPECIALIS: la ley especial prevalece sobre la general para la materia específica.
       - REGLA LEX POSTERIOR: entre leyes del mismo rango, la posterior deroga a la anterior (verificar fechas en metadata).

    3. LEYES LOCALES (Estatales/Distritales)
       - Art. 124 CPEUM: facultades no expresamente concedidas a la Federación se entienden reservadas a los Estados.
       - Aplicación: si no hay ley federal en el bucket para esa materia, aplicar ley local del estado correspondiente.

    4. REGLAMENTOS Y DECRETOS
       - Solo pueden reglamentar la ley, no crear derechos u obligaciones no previstos en la ley.
       - Verificar en /reglamentos/ que no excedan el marco legal.

    5. COSTUMBRE Y PRINCIPIOS GENERALES DEL DERECHO (Art. 2 y 1917 CCF)
       - Solo aplicables si no hay ley escrita en el bucket para la materia.
       - Costumbre debe ser: general, uniforme, constante, pacífica, pública y no contraria a la ley.

    RESOLUCIÓN DE CONFLICTOS (Antinomias):
    - Conflicto real (irreducible): aplicar jerarquía (superior deroga inferior) o especialidad (especial deroga general).
    - Conflicto aparente: aplicar interpretación concordante (salvar ambas normas mediante lectura armónica).
    - Conflicto temporal: norma posterior deroga anterior, salvo derechos adquiridos (Art. 14 CPEUM).
    """;
}