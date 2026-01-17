    package com.juxa.legal_advice.util;

    import com.juxa.legal_advice.dto.UserDataDTO;

    public class PromptBuilder {

        /**
         * Prompt para el Diagnóstico Inicial (Ajustado al diagrama de flujo5)
         */
        private static final String JUXIA_BASE_INSTRUCTIONS = """
                Eres Juxa Asistente Legal, la inteligencia artificial de asistencia legal líder en Méxicxo. 
                Tu propósito es ayudar con legalidad, claridad y humanidad, siguiendo los principios de la UNESCO.
                
                REGLAS DE IDENTIDAD Y LENGUAJE (LECTURA FÁCIL):
                1. Sé empática: Valida las emociones del usuario antes de dar cualquier consejo legal.
                2. No inventes: Si no conoces una ley o dato, admítelo. Nunca inventes hechos o códigos.
                3. Claridad (SCJN): Usa oraciones simples (Sujeto + Verbo + Predicado). Evita tecnicismos como 'litis' o 'foja'.
                4. Sentido de Urgencia: Si detectas riesgo a la integridad física o vulnerabilidad extrema, prioriza la seguridad y el 911.
                5. TPROHIBIDO decir "El usuario", "La persona" o "El caso de %s".\s
                - Di: "Tú me cuentas...", "Entiendo que te sientes...", "Tus derechos son...".
                6. LECTURA CLARA: Basa tu comunicación en la 'Guía para elaborar sentencias en formato de lectura fácil'.
                7.Si la consulta no está relacionada con el ámbito legal, menciona que no está dentro de tu jurisdicción.
                
                AVISO DE TRANSPARENCIA OBLIGATORIO (Art. 50 AI Act.):
                        - Identifícate claramente como un Sistema de IA, no como un humano.
                        - Indica que tus respuestas no son asesoría jurídica vinculante y requieren validación profesional humana.
                        ""\"
                
                """;

        private static final String RESPONSE_FORMAT = """
        REGLAS DE SALIDA (JSON ESTRICTO):
                1. Campo "text": Análisis empático dirigido a la persona (entre 400 y 500 caracteres).\s
                               - NO uses puntos suspensivos. Termina la idea.
                            2. Campo "suggestions": Proporciona EXACTAMENTE 3 preguntas (ni más, ni menos).
                               - Las preguntas deben ser directas ("¿Tú...?", "¿Dónde estás...?").
                
                            {
                              "text": "Mensaje de 400-500 caracteres hablando de TÚ al usuario...",
                              "suggestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"],
                              "downloadPdf": false
                            }
                            """;


        public static String buildInitialDiagnosisPrompt(UserDataDTO userData, String contextoPersona) {
            String descripcion = (userData.getDescription() != null) ? userData.getDescription() : "";
            boolean esNuevoChat = descripcion.isEmpty() || descripcion.length() < 15;

            String misiónLegal = esNuevoChat
                    ? "MISION: Presenta el Aviso de Transparencia de JUXA.IO. Explica que eres una IA, que la finalidad es informativa y que se recomienda supervisión humana."
                    : "MISION: Realiza un triaje legal empático y profesional basado en los hechos narrados.";

            return String.format("""
            %s
            
            %s
            
            INTEROLOCUTOR ACTUAL: %s.
          
            
            REGLA DE SALIDA: No uses puntos suspensivos (...) para cortar ideas. Termina tus párrafos e ideas.
            INSTRUCCIÓN FINAL: Dirígete a %s por su nombre y háblale de tú.\s
                                            RESPONDE ÚNICAMENTE EN JSON.
                                            ""\",
            {
              "text": "Tu mensaje inicial incluyendo el disclaimer legal y análisis empático...",
              "suggestions": ["Pregunta sobre dato faltante 1", "Pregunta 2", "Pregunta 3"],
              "downloadPdf": %b
            }
            """,
                    String.format(JUXIA_BASE_INSTRUCTIONS, userData.getName()),
                    misiónLegal, userData.getName(), contextoPersona, descripcion,
                    "dictamen".equalsIgnoreCase(userData.getDiagnosisPreference()));
        }
        /**
         * Prompt para el Chat Interactivo (Optimizado para RAG con Vertex AI)
         */
        public static String buildInteractiveChatPrompt(
                String reglasHojaDeRuta, String contextoDocs, String contextoUsuario,
                String historial, String mensajeActual) {

            return String.format("""
            %s
            
            REGLAS (Hoja_deRita): %s
            CONOCIMIENTO TÉCNICO (RAG): %s
            CONTEXTO CLIENTE: %s
            HISTORIAL: %s
            MENSAJE ACTUAL DEL USUARIO: "%s"
            
            INSTRUCCIÓN ESPECIAL (BOTÓN DE ALERTA):
            Si detectas extrema gravedad (orden de aprehensión, violencia física o plazos que vencen hoy), 
            debes iniciar el campo 'text' con: " ESTA ES UNA CONSULTA CRÍTICA. JUXA.IO LE INSTA A CONTACTAR A UN ABOGADO DE INMEDIATO."
            
            INSTRUCCIÓN DE SALIDA:
            - Brinda un análisis extenso en 'text' siguiendo el método de Lectura Fácil.
            - Ofrece ÚNICAMENTE 3 sugerencias sobre información que el usuario aún NO ha proporcionado.
            
            {
              "text": "Respuesta detallada y empática...",
              "suggestions": ["Pregunta crítica 1", "Pregunta 2", "Pregunta 3"],
              "downloadPdf": false
            }
            """,
                    JUXIA_BASE_INSTRUCTIONS, reglasHojaDeRuta, contextoDocs, contextoUsuario, historial, mensajeActual);
        }
        public static String buildHarmonizedPrompt(UserDataDTO user, String contextoLegal) {
            StringBuilder prompt = new StringBuilder();

            // Inyecta Identidad Base
            prompt.append(String.format(JUXIA_BASE_INSTRUCTIONS, user.getName()));

            // 3. CONTEXTO OPERATIVO (DATOS DINÁMICOS)
            // Solo enviamos lo que la IA realmente necesita procesar.
            prompt.append(String.format("""
                \nDATOS DE LA PERSONA CON LA QUE HABLAS (%s):
                - Tu ubicación: %s
                - Tu estado de proceso: %s
                - Lo que tú nos narras: "%s"
                """, user.getName(), user.getLocation(), user.getProcessStatus(), user.getDescription()));

            if (Boolean.TRUE.equals(user.getHasViolence())) {
                prompt.append("\nALERTA: Detecto que sufres violencia. Prioriza su seguridad en tu respuesta.");
            }

            prompt.append("\nCONOCIMIENTO TÉCNICO PARA APOYARTE:\n").append(contextoLegal);
            prompt.append("\n").append(RESPONSE_FORMAT);

            return prompt.toString();
        }
        }

