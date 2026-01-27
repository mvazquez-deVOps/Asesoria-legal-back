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
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken.Payload;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value; // Para leer el Client ID
import java.util.Collections; // Para Collections.singletonList
import java.util.Optional;    // Para Optional<UserEntity>

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
    @Value("${google.client.id}")
    private String googleClientId;

    public AuthResponseDTO verifyGoogleToken(String idTokenString) throws Exception {
        // 1. Configuramos el verificador de Google usando tu Client ID (Variable de entorno)
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        // 2. Validamos el token físicamente con los servidores de Google
        GoogleIdToken idToken = verifier.verify(idTokenString);

        if (idToken != null) {
            Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            String name = (String) payload.get("name");

            // 3. Buscamos si el usuario ya existe en tu base de datos 'diagnosis'
            Optional<UserEntity> userOptional = userRepository.findByEmail(email);
            UserEntity user;

            if (userOptional.isPresent()) {
                user = userOptional.get();
            } else {
                // 4. Si es nuevo, lo creamos con valores por defecto (igual que en tu register)
                user = new UserEntity();
                user.setName(name);
                user.setEmail(email);
                user.setPassword("GOOGLE_AUTH_EXTERNAL"); // No necesita password real
                user.setPersonType("FISICA"); // Valor por defecto
                user.setSubscriptionPlan("FREE");
                user.setLoginCount(1);
                user.setRole("USER");
                userRepository.save(user);
            }

            // 5. Generamos el token de JUXA para que el Frontend lo guarde
            String token = jwtUtil.generateToken(user.getEmail());

            // 6. Retornamos el DTO completo (exactamente como lo tienes en register)
            return AuthResponseDTO.builder()
                    .token(token)
                    .userId(user.getId().toString())
                    .email(user.getEmail())
                    .name(user.getName())
                    .loginCount(user.getLoginCount())
                    .role(user.getRole())
                    .subscriptionPlan(user.getSubscriptionPlan())
                    .personType(user.getPersonType())
                    .build();
        } else {
            throw new RuntimeException("El token de Google no es válido o ha expirado.");
        }
    }


}