package com.juxa.legal_advice.service;
import lombok.extern.slf4j.Slf4j; // Agrega esto
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendConfirmationEmail(String toEmail, String token) {
        // En producción, cambia localhost por tu dominio real (ej. https://juxa.com/api/auth/confirm?token=)
        String confirmationUrl = "http://localhost:8080/api/auth/confirm?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("tu-correo@gmail.com"); // Reemplaza con el correo configurado en tu application.properties
        message.setTo(toEmail);
        message.setSubject("Confirma tu cuenta en Juxa");
        message.setText("¡Bienvenido a Juxa!\n\nPor favor, confirma tu cuenta haciendo clic en el siguiente enlace:\n"
                + confirmationUrl + "\n\nEste enlace expirará en 24 horas.");

        mailSender.send(message);
        }
}