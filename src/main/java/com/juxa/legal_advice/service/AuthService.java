package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil; // <--- Importante inyectar esto

    public AuthResponseDTO login(AuthRequestDTO request) {
        UserEntity user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        // GENERACIÓN DE TOKEN REAL (Ya no más "provisional")
        String token = jwtUtil.generateToken(user.getEmail());

        // Reemplaza el bloque del return por este:
            return new AuthResponseDTO(
                    token,
                    user.getId().toString(),
                    user.getEmail(),
                    user.getName(),
                    user.getLoginCount(),
                    user.getRole(),
                    user.getSubscriptionPlan(),
                    user.getPersonType()
            );
        }
}