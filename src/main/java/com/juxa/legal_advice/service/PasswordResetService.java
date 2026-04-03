package com.juxa.legal_advice.service;

import com.juxa.legal_advice.model.PasswordResetOtp;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.PasswordResetOtpRepository;
import com.juxa.legal_advice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetOtpRepository otpRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    private static final int OTP_EXPIRATION_MINUTES = 15;

    @Transactional
    public void requestPasswordReset(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado")); // Maneja tus excepciones personalizadas

        // 1. Borrar OTPs anteriores para este usuario (opcional pero recomendado)
        otpRepository.deleteByUser(user);

        // 2. Generar un código numérico de 6 dígitos seguro
        String otp = generateNumericOtp();

        // 3. Guardar el OTP en la base de datos
        PasswordResetOtp passwordResetOtp = new PasswordResetOtp(otp, user, OTP_EXPIRATION_MINUTES);
        otpRepository.save(passwordResetOtp);

        // 4. Enviar el correo
        emailService.sendPasswordRecoveryOtp(user.getEmail(), otp);
    }

    @Transactional
    public void resetPassword(String email, String otp, String newPassword) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 1. Buscar el OTP válido para este usuario
        PasswordResetOtp resetOtp = otpRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Código inválido o incorrecto"));

        // 2. Verificar si expiró
        if (resetOtp.getExpiryDate().isBefore(LocalDateTime.now())) {
            otpRepository.delete(resetOtp); // Limpieza
            throw new RuntimeException("El código ha expirado");
        }

        // 3. Actualizar la contraseña (¡siempre hasheada!)
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 4. Borrar el OTP usado por seguridad
        otpRepository.delete(resetOtp);
    }

    // Helper para generar 6 dígitos aleatorios usando criptografía segura
    private String generateNumericOtp() {
        SecureRandom random = new SecureRandom();
        int num = random.nextInt(1000000); // Rango: 0 - 999999
        return String.format("%06d", num); // Asegura que siempre tenga 6 dígitos (ej. 004521)
    }

    // Agrega este método a tu PasswordResetService existente

    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        // 1. Buscar al usuario
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Buscar el OTP para este usuario
        PasswordResetOtp resetOtp = otpRepository.findByOtpAndUser(otp, user)
                .orElseThrow(() -> new RuntimeException("Código inválido o incorrecto"));

        // 3. Verificar si expiró
        if (resetOtp.getExpiryDate().isBefore(LocalDateTime.now())) {
            // Aquí podrías borrar el OTP expirado si lo deseas, o dejar que una tarea programada lo limpie después
            throw new RuntimeException("El código ha expirado");
        }

        // Si llega hasta aquí sin lanzar excepciones, el código es válido.
        // NOTA: No borramos el OTP de la base de datos todavía.
    }


}