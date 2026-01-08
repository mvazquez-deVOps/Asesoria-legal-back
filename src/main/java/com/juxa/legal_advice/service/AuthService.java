package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO; // Usamos el DTO que ya existe
import com.juxa.legal_advice.model.UserEntity; // Tu entidad real
import com.juxa.legal_advice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor // Esto soluciona la inyección de userRepository
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public String login(AuthRequestDTO request) {
        // 1. Buscar al usuario por email usando tu entidad
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. VALIDACIÓN REAL
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // 3. Token simulado para la v1.0.1 (mientras creas el JwtService)
        return "token-provisional-101";
    }
}