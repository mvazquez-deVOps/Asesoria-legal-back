package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.stereotype.Service;

@Service
public class WhatsappService {
    /**
     * Envía un lead legal por WhatsApp usando los datos del diagnóstico.
     */
    public void sendLead(DiagnosisEntity entity) {
        String phone = entity.getPhone();
        String message = buildMessage(entity);

        // Aquí iría la integración con la API de WhatsApp (Twilio, Meta, etc.)
        System.out.printf("📲 Enviando mensaje a %s:\n%s\n", phone, message);
    }

    /**
     * Construye el mensaje legal que se enviará por WhatsApp.
     */
    private String buildMessage(DiagnosisEntity entity) {
        return """
            Hola %s, gracias por confiar en Asesoría Legal Integral.

            Hemos recibido tu caso sobre: %s
            Cuantía estimada: %s MXN
            Jurisdicción: %s
            Contraparte: %s
            Estatus actual: %s

            Nuestro equipo legal está analizando tu situación. Pronto recibirás tu dictamen preliminar.
            """.formatted(
                entity.getName(),
                entity.getDescription(),
                entity.getAmount(),
                entity.getLocation(),
                entity.getCounterparty(),
                entity.getProcessStatus()
        );
    }



}
