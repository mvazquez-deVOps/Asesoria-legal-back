package com.juxa.legal_advice.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendConfirmationEmail(String toEmail, String token) {
        // En producción, cambia localhost por tu dominio real (ej. https://juxa.com/api/auth/confirm?token=)
        String confirmationUrl = "http://localhost:8080/api/auth/confirm?token=" + token;
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Confirma tu cuenta en Juxa");
        message.setText("¡Bienvenido a Juxa!\n\nPor favor, confirma tu cuenta haciendo clic en el siguiente enlace:\n"
                + confirmationUrl + "\n\nEste enlace expirará en 24 horas.");

        mailSender.send(message);
    }

    public void sendPasswordRecoveryOtp(String toEmail, String otp) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Código de recuperación de contraseña - Juxa");
        message.setText("Hola,\n\n"
                + "Hemos recibido una solicitud para restablecer tu contraseña.\n"
                + "Tu código de recuperación es: " + otp + "\n\n"
                + "Este código expirará en 15 minutos. Si no solicitaste este cambio, ignora este correo.");

        mailSender.send(message);
        log.info("Correo de recuperación enviado a: {}", toEmail);
    }
}