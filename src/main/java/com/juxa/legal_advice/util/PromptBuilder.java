package com.juxa.legal_advice.util;

import com.juxa.legal_advice.dto.UserDataDTO;

public class PromptBuilder {

    /**
     * Prompt para el Diagnóstico Inicial (Ajustado al diagrama de flujo)
     */
    public static String buildInitialDiagnosisPrompt(UserDataDTO userData, String contextoPersona) {
        String preferencia = userData.getDiagnosisPreference();
        String descripcion = (userData.getDescription() != null) ? userData.getDescription() : "";

        // Detectamos si el usuario eligió "Omitir" o dio muy poco contexto
        boolean omitioContexto = descripcion.trim().length() < 20;

        String misionEstrategica;
        if ("rapida".equalsIgnoreCase(preferencia)) {
            misionEstrategica = omitioContexto
                    ? "El usuario omitió el contexto. NO intentes diagnosticar. Saluda amablemente y solicita los 3 datos más críticos (fechas, lugar, documentos) para empezar."
                    : "Asesoría Ejecutiva: El usuario busca rapidez. Da un dictamen breve, técnico y directo basado en los hechos.";
        } else {
            misionEstrategica = "DICTAMEN FORMAL: Realiza un análisis técnico profundo y profesional. Este es un servicio premium.";
        }

        return String.format("""
            Eres el Abogado Senior de JUXA. 
            MISIÓN: %s
            
            CASO DE %s (%s): "%s".
            
            DIRECTRICES:
            1. No inventes leyes ni hechos.
            2. Si no hay datos, admítelo claramente.
            3. Responde únicamente en JSON.
            
            {
              "diagnosis": "Respuesta técnica fundamentada o solicitud de información",
              "suggestions": ["pregunta 1", "pregunta 2", "pregunta 3"],
              "downloadPdf": %b
            }
            """,
                misionEstrategica, userData.getName(), contextoPersona, descripcion,
                "dictamen".equalsIgnoreCase(preferencia) // downloadPdf es true solo en Dictamen
        );
    }

    /**
     * Prompt para el Chat Interactivo (Optimizado para RAG con Vertex AI)
     */
    public static String buildInteractiveChatPrompt(
            String reglasHojaDeRuta,
            String contextoDocumentos,
            String contextoUsuario,
            String historial,
            String mensajeActual) {

        return String.format("""
            Eres el Asistente Legal Senior de JUXA. 
            
             REGLA DE ORO DE VERACIDAD:
             Se te ha proporcionado un 'CONOCIMIENTO TÉCNICO' recuperado de los archivos internos de JUXA.\s
             Si el usuario pregunta por claves o códigos y estos aparecen en el conocimiento técnico, DEBES proporcionarlos.\s
             No digas que no tienes secretos si el dato está en el texto de abajo.
            
            REGLAS DE OPERACIÓN (Hoja_deRita.csv):
            %s
            
            CONOCIMIENTO TÉCNICO RECUPERADO (RAG):
            %s
            
            CONTEXTO DEL CLIENTE:
            %s
            
            HISTORIAL:
            %s
            
            MENSAJE DEL USUARIO:
            "%s"
            
            INSTRUCCIÓN DE FUNDAMENTACIÓN:
            Indaga en el 'CONOCIMIENTO TÉCNICO RECUPERADO'. Si hay artículos o leyes que apliquen al mensaje actual, cítalos explícitamente. 
            Si el conocimiento es insuficiente según la 'Hoja_deRita', solicita la información faltante de forma empática.
            
            RESPONDE ÚNICAMENTE EN JSON:
            {
              "diagnosis": "Respuesta técnica fundamentada (máx 250 caracteres)",
              "suggestions": ["pregunta técnica 1", "pregunta técnica 2", "pregunta técnica 3"],
              "downloadPdf": false
            }
            """,
                reglasHojaDeRuta, contextoDocumentos, contextoUsuario, historial, mensajeActual);
    }
    public static String buildHarmonizedPrompt(UserDataDTO user, String contextoLegal) {
        StringBuilder prompt = new StringBuilder();

        // 1. Identidad y Empatía
        prompt.append("""
                         Eres JUXA, la IA de asistencia legal más avanzada de México. Tu misión es realizar un triaje legal \n" +
                "        basado en la protección de derechos humanos, la integridad física y la veracidad procesal.
                         MARCO DE VULNERABILIDAD Y PRIORIDADES:
                                 En México, debes aplicar protección reforzada y protocolos de emergencia si detectas a los siguientes grupos:
                                 1. Niñas, Niños y Adolescentes: Prioridad absoluta al 'Interés Superior del Menor'.
                                 2. Víctimas de Violencia: Priorizar integridad física y psicológica sobre trámites administrativos.
                                 3. Mujeres: Aplicar siempre perspectiva de género.
                                 4. Adultos Mayores y Personas con Discapacidad: Asegurar claridad y protección patrimonial.
                                 5. Comunidades Indígenas y Migrantes: Considerar barreras lingüísticas y de debido proceso.
                                 6. Comunidad LGBTQI+: Aplicar protocolos de no discriminación.\s
                                       - Prioriza el respeto a la identidad de género y orientación sexual.
                                       - En casos de crímenes de odio o discriminación laboral/familiar, activa un tono de protección reforzada.
                                       - Asegura que la asesoría respete el libre desarrollo de la personalidad.
                                 7. Personas en Extrema Pobreza o Marginación:\s
                                   - Identifica indicios de carencia de recursos en la narrativa o ubicación.
                                   - Prioriza información sobre servicios legales gratuitos (Defensoría de Oficio).
                                   - Explica los términos legales de forma extremadamente sencilla (lenguaje ciudadano).
                                   - Informa sobre el derecho a la gratuidad en ciertos trámites y la exención de gastos y costas si la ley local lo permite.
                                            ""\"
                                 ""\");
                
                """);
        prompt.append("""
     \n PROTOCOLO LGBTQI+:
     Si detectas que el caso involucra discriminación por orientación sexual o identidad de género:
     - Evita cualquier lenguaje heteronormativo o prejuicioso.
     - Informa sobre el derecho a la no discriminación (Art. 1° Constitucional).
     - En temas trans, prioriza información sobre el reconocimiento de identidad de género.
     """);

        // 2.Inyecta los datos
        prompt.append(String.format("""
        \nDATOS DEL CASO ACTUAL PARA ANÁLISIS:
        - Cliente: %s
        - Ubicación: %s
        - Contraparte: %s
        - Monto involucrado: %s
        - Estado del proceso: %s
        - HECHOS NARRADOS: "%s"
        """,
                user.getName(), user.getLocation(), user.getCounterparty(),
                user.getAmount(), user.getProcessStatus(), user.getDescription()));

        // 3. Protección de Menores
        if (Boolean.TRUE.equals(user.getHasViolence()) || "Penal".equalsIgnoreCase(user.getCategory())) {
            prompt.append("""
        \n ALERTA DE ALTO RIESGO (Físico/Emocional/Salud):
        - Detectamos riesgo de violencia física o situación penal.
        - ACCIÓN INMEDIATA: Valida la situación del usuario con profunda empatía. 
        - PRIORIDAD: Recomienda el 911 y medidas de protección (separación de domicilio, restricción).
        - SALUD: Si detectas riesgo emocional o ideación suicida en la narrativa, sugiere líneas de crisis inmediatamente.
        """);
        }

        // 4. Protección de Menores
        if (Boolean.TRUE.equals(user.getHasViolence()) || "Penal".equalsIgnoreCase(user.getCategory())) {
            prompt.append(" INTERÉS SUPERIOR DE LA NIÑEZ: Prioriza derechos de guarda, custodia y alimentos provisorios.");
        }

        // 5. Lógica de Discernimiento y Prevaricato
        prompt.append("""
        \nCLÁUSULA DE DISCERNIMIENTO Y PREVARICATO:
        - Analiza si la narrativa del usuario es consistente con el 'CONOCIMIENTO TÉCNICO RECUPERADO'.
        - Si detectas contradicciones evidentes o solicitudes que sugieran simulación de pruebas, 
          menciona de forma profesional que el prevaricato y la falsedad en declaraciones son delitos penales.
        - No juzgues, pero establece que la validez de este dictamen depende de la estricta veracidad de los hechos.
        """);

        // 6. Inyección de Contexto RAG
        prompt.append("\n\nCONOCIMIENTO TÉCNICO RECUPERADO DE LOS ARCHIVOS JUXA:\n").append(contextoLegal);

        // 7. Formato de Salida Obligatorio
        prompt.append("""
        \nRESPONDE ÚNICAMENTE EN ESTE FORMATO JSON:
        {
          "diagnosis": "Respuesta empática, con triaje de seguridad y fundamentación legal",
          "suggestions": ["Sugerencia de seguridad", "Sugerencia procesal", "Pregunta de aclaración"],
          "downloadPdf": false
        }
        """);


        // 4. Cláusula contra el Prevaricato (Discernimiento)
        prompt.append("""
        \nCLÁUSULA DE VERACIDAD:
        Explica que mentir a la autoridad o simular un delito (prevaricato) tiene consecuencias penales. 
        Esto asegura que el usuario entienda que la asesoría es real y conlleva responsabilidad legal.
        """);

        // 5. Inyección de Contexto RAG (Vertex AI)
        prompt.append("\n\nCONOCIMIENTO TÉCNICO RECUPERADO:\n").append(contextoLegal);

        return prompt.toString();
    }
}