package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.DiagnosisEntity;
import org.springframework.stereotype.Service;

@Service
public class WhatsappService {

    public void sendDiagnosisNotification(DiagnosisEntity entity) {
        // Como ya no hay campo 'phone' en la entidad, enviamos a un log o
        // usamos un dato genérico mientras se implementa la relación con User
        System.out.println("Enviando notificación de WhatsApp para el diagnóstico ID: " + entity.getId());

        String message = String.format(
                "Hola! Tu diagnóstico JUXA está listo.\nID: %s\nDescripción: %s",
                entity.getId(),
                entity.getDescription()
        );

        // Aquí iría tu lógica de cliente de WhatsApp (Twilio/Meta)
        System.out.println("Mensaje: " + message);
    }
}