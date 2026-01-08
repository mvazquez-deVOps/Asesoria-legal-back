package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.util.JwtUtil; // Asegúrate de importar tu utilidad de JWT
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil; // Inyectamos el componente que ya tienes creado
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO credentials) {
        // 1. Buscar usuario
        UserEntity user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Validar con BCrypt
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // 3. Incrementar contador de login
        user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);

        // 4. Bypass de pruebas para dominio juxa (opcional, pero útil para tu Admin)
        if (user.getEmail().endsWith("@juxa.mx")) {
            user.setSubscriptionPlan("PREMIUM");
            user.setRole("ADMIN");
        }

        userRepository.save(user);

        // 5. Generar Token
        String token = jwtUtil.generateToken(user.getEmail());

        // 6. Devolver el DTO con los 7 campos que el Dashboard espera
        return new AuthResponseDTO(
                token,
                user.getId().toString(),
                user.getEmail(),
                user.getName(),
                user.getLoginCount(),
                user.getRole(),
                user.getSubscriptionPlan()
        );
    }

    public UserDataDTO getUserById(String id) {
        return userRepository.findById(Long.parseLong(id))
                .map(user -> {
                    UserDataDTO dto = new UserDataDTO();
                    dto.setUserId(user.getId().toString());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setLoginCount(user.getLoginCount()); // Sincronizamos con el DTO
                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}