package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserRegistrationDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponseDTO register(UserRegistrationDTO dto) {
        // 1. Primero creamos el objeto vacío
        UserEntity user = new UserEntity();

        // 2. Le pasamos los datos que vienen del Frontend (DTO)
        user.setName(dto.getName());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());

        // 3. Encriptamos la contraseña (SEGURIDAD)
        user.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. AQUÍ VAN LAS LÍNEAS QUE PREGUNTASTE (Valores por defecto)
        // Esto evita que la base de datos 'diagnosis' rechace el registro
        user.setPersonType("FISICA");
        user.setSubscriptionPlan("FREE");
        user.setLoginCount(0);
        user.setRole("USER");

        // 5. Ahora sí, guardamos en la base de datos 'diagnosis'
        userRepository.save(user);

        // 6. Generamos el token para que entre directo
        String token = jwtUtil.generateToken(user.getEmail());

        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .loginCount(user.getLoginCount())
                .role(user.getRole())
                .subscriptionPlan(user.getSubscriptionPlan())
                .personType(user.getPersonType())
                .phone(user.getPhone()) // si agregaste el campo en el DTO
                .build();
    }

}