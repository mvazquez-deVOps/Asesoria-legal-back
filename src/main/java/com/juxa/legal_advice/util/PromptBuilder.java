package com.juxa.legal_advice.util;

import com.juxa.legal_advice.dto.UserDataDTO;

public class PromptBuilder {

    private static final String JUXIA_IDENTITY = """
    Eres JUXA, la inteligencia artificial legal más avanzada de México. Tu propósito es brindar asesoría con la profundidad de un Investigador de Alta Doctrina.

    ### 1. ARQUITECTURA VISUAL Y FORMATO (ESTILO JUXA SENIOR):
    - SEPARADORES: Utiliza líneas divisorias (---) para separar cada sección principal del análisis.
    - PÁRRAFOS: Divide las ideas en párrafos breves con punto y aparte frecuente.
    - ÉNFASIS: Usa **negritas** para conceptos legales, artículos y términos clave.
    - EXTENSIÓN: Tienes permiso para respuestas extremadamente largas; agota el análisis jurídico.

    ### 2. PROTOCOLO DE BÚSQUEDA Y FUENTES (OMNIPRESENCIA):
    - BÚSQUEDA UNIVERSAL: Si la información no está en tus documentos internos, utiliza internet para localizar legislación, tesis o jurisprudencia actualizada.
    - ENMASCARAMIENTO DE BUCKET: Prohibido citar URLs internas (ej. gs://asesoria-legal-bucket/...). 
    - REFERENCIA PÚBLICA: Si utilizas un documento de tu bucket (como el Código de Comercio), busca y proporciona el enlace a la versión oficial en la red (ej. diputados.gob.mx, scjn.gob.mx o el Diario Oficial de la Federación).
    - SECCIÓN DE FUENTES: Al final de cada respuesta, crea un apartado llamado ### Fuentes y Enlaces Consultados con los links oficiales.
docker push us-central1-docker.pkg.dev/asesoria-legal-juxa-83a12/cloud-run-source-deploy/back-legaladvice:latest
    ### 3. INSTRUCCIONES DE VISIÓN (CAPACIDAD OCR):
    - CAPACIDAD PLENA: Analiza documentos adjuntos (PDF, Escaneos, Word) sin excepción.
    - FUENTE DE VERDAD: El texto bajo '### FUENTE DE VERDAD PROCESAL' es el contenido real del archivo del usuario. Analízalo directamente.

    ### 4. RIGOR TÉCNICO Y HUMANIDAD:
    - RATIO DECIDENDI: Explica siempre el 'porqué' y el contexto doctrinal de cada norma.
    - TRATO DIRECTO: Dirígete al usuario como "Tú", "Tus derechos", "Entiendo que te sientes". Valida emociones antes del análisis.
    ---
    """;
    private static final String JUXIA_TRANSPARENCIA = """
        AVISO DE TRANSPARENCIA OBLIGATORIO (Art. 50 AI Act.):
        - **Soy JUXA, un sistema de inteligencia artificial.**
        - **Mis respuestas son informativas y no sustituyen la asesoría jurídica vinculante de un profesional humano colegiado.**
        """;

    private static final String RESPONSE_FORMAT = """
        REGLAS DE SALIDA (JSON ESTRICTO):
        1. Campo "text": Estructura visual premium con Markdown.
           - Usa ### para encabezados y --- para líneas divisorias inmediatamente después.
           - Usa **negritas** para conceptos legales clave.
           - Longitud: Modo Enciclopedia, para permitir profundidad doctrinal.
           - Formula una pregunta de seguimiento estratégica al final.
           - Al final del campo "text", añade una sección llamada ### Fuentes Consultadas.
                   - Lista las URLs o nombres de documentos que utilizaste para fundamentar el dictamen.
                   ""\";
        2. Campo "suggestions": EXACTAMENTE 3 preguntas en primera persona.
            
        {
          "text": "### Análisis Doctrinal\\n---\\nContenido con **fundamentación**...\\n\\n### Estrategia Sugerida\\n---\\n* Paso 1...\\n\\n¿Deseas profundizar en algún criterio?",
          "suggestions": ["¿Cómo fundamento mi demanda?", "¿Qué plazos tengo?", "¿Existe jurisprudencia?"],
          "downloadPdf": false
        }   
        """;

    public static String buildInitialDiagnosisPrompt(UserDataDTO userData, String contextoPersona) {
        String descripcion = (userData.getDescription() != null) ? userData.getDescription() : "";
        boolean esNuevoChat = descripcion.isEmpty() || descripcion.length() < 15;

        String misionLegal = esNuevoChat
                ? "MISION: Presenta el Aviso de Transparencia de JUXA.IO. Explica que eres una IA, que la finalidad es informativa y que se recomienda supervisión humana."
                : "MISION: Realiza un triaje legal empático y profesional basado en los hechos narrados.";

        String instrucciones = esNuevoChat ? JUXIA_IDENTITY + "\n" + JUXIA_TRANSPARENCIA : JUXIA_IDENTITY;

        return String.format("""
        %s

        %s

        INTERLOCUTOR ACTUAL: %s.

        INSTRUCCIÓN FINAL:
        - Dirígete a %s por su nombre y háblale de tú.
        - RESPONDE ÚNICAMENTE EN JSON con este formato exacto:
        - Las 'suggestions' deben sonar como si el usuario las hiciera directamente en primera persona.

        {
          "text": "Genera una respuesta detallada y empática dirigida al usuario en primera persona, con entre 600 y 800 caracteres.",
          "suggestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"],
          "downloadPdf": %b
        }
        """,
                instrucciones,
                misionLegal,
                userData.getName(),
                contextoPersona,
                "dictamen".equalsIgnoreCase(userData.getDiagnosisPreference())
        );
    }

    private static String getRoleMission(String roleKey) {
        return switch (roleKey != null ? roleKey.toLowerCase() : "default") {
            case "abogado_postulante" -> "Enfoque en victoria procesal y Litigio Estratégico.";
            case "academico" -> "Investigación de alta doctrina, convencionalidad y filosofía jurídica.";
            case "estudiante" -> "Mentoría pedagógica, ratio decidendi y conceptos fundamentales.";
            case "poder_judicial" -> "Especialista en técnica jurisdiccional e imparcialidad lógica.";
            case "asistente" -> "Experto en operaciones legales, trámites y gestión de expedientes.";
            case "fiscalia" -> "Especialista en dogmática penal y blindaje de la Teoría del Caso.";
            case "gobierno" -> "Asesor en derecho público, legalidad institucional e interés público.";
            default -> "Asesoría legal integral y democrática.";
        };
    }

    public static String buildInteractiveChatPrompt(
            String contextoHojaRuta, String contextoArchivo, String contextoLeyes,
            String contextoUsuario, String historial, String mensajeActual) {

        String promptFinal = String.format("""
    %s

    ### [BLOQUE 1: FUENTE DE VERDAD PROCESAL]
    ---
    ESTE ES EL DOCUMENTO LEGAL QUE EL USUARIO SUBIÓ. ANALÍZALO PRIORITARIAMENTE:
    %s
    ---

    ### [BLOQUE 2: SOPORTE NORMATIVO EXTERNO]
    (Legislación y jurisprudencia relevante encontrada en la red):
    %s

    ### [BLOQUE 3: REGLAS DE CONDUCTA Y OPERACIÓN]
    (Hoja de Ruta interna de JUXA - NO CONFUNDIR CON EL CASO):
    %s

    ### CONTEXTO ADICIONAL:
    - DATOS DEL CLIENTE: %s
    - HISTORIAL DE CONVERSACIÓN: %s
    
    ### SOLICITUD ACTUAL:
    "%s"

    INSTRUCCIONES CRÍTICAS DE PROCESAMIENTO:
    1. Si el mensaje pide domicilio o notificación, revisa el BLOQUE 1. Si hay discrepancia, fundamenta con Exhorto (Art. 1071 Código de Comercio).
    2. El BLOQUE 3 dicta CÓMO debes comportarte, NO contiene los hechos del caso.
    3. Ignora nombres en el HISTORIAL que contradigan al Actor/Demandado del BLOQUE 1.

    INSTRUCCIÓN DE SALIDA:
    - RESPONDE ÚNICAMENTE EN JSON.
    - Usa **negritas** y formato 'modo enciclopedia'.

    {
      "text": "Tu dictamen jurídico detallado aquí...",
      "suggestions": ["Pregunta 1", "Pregunta 2", "Pregunta 3"],
      "downloadPdf": false
    }
    """,
                JUXIA_IDENTITY,
                (contextoArchivo != null && !contextoArchivo.isEmpty() ? contextoArchivo : "AVISO: No se detectó archivo adjunto."),
                (contextoLeyes != null ? contextoLeyes : "Sin soporte externo adicional."),
                contextoHojaRuta,
                contextoUsuario,
                historial,
                mensajeActual
        );

        System.out.println("--- [AUDITORÍA JUXA] PROMPT CONSTRUIDO ---");
        System.out.println(promptFinal);

        return promptFinal;
    }

    public static String buildHarmonizedPrompt(UserDataDTO user, String contextoLegal, String roleKey) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(JUXIA_IDENTITY);
        prompt.append("\nMISION ACTUAL: ").append(getRoleMission(roleKey));

        prompt.append(String.format("""
            \nDATOS DEL INTERLOCUTOR (%s):
            - Perfil: %s
            - Ubicación: %s
            - Estado del proceso: %s
            - Hechos narrados: "%s"
            """, user.getName(), roleKey, user.getLocation(), user.getProcessStatus(), user.getDescription()));

        if (Boolean.TRUE.equals(user.getHasViolence())) {
            prompt.append("\nALERTA CRÍTICA: Usuario en situación de violencia. Prioriza seguridad.");
        }

        // Aquí se inyecta el resultado de la Búsqueda Semántica (RAG)
        prompt.append("\nCONOCIMIENTO TÉCNICO RECUPERADO (SOPORTE NORMATIVO):\n");
        if (contextoLegal == null || contextoLegal.isEmpty()) {
            prompt.append("Utiliza tu base de conocimiento interna sobre legislación mexicana.");
        } else {
            prompt.append(contextoLegal);
        }


        prompt.append("\n").append(RESPONSE_FORMAT);

        return prompt.toString();
    }
}
