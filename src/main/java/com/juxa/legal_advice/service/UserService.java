package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.dto.UserRegistrationDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // Inyectado desde SecurityConfig

    /**
     * Proceso de Login y Autenticación
     */
    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO credentials) {
        // 1. Buscar usuario por email
        UserEntity user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new RuntimeException("El correo electrónico no está registrado"));

        // 2. Validar contraseña con BCrypt
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new RuntimeException("La contraseña es incorrecta");
        }

        // 3. Actualizar estadísticas de sesión
        user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);

        // 4. Lógica de privilegios para dominio JUXA
        if (user.getEmail().toLowerCase().endsWith("@juxa.mx")) {
            user.setSubscriptionPlan("PREMIUM");
            user.setRole("ADMIN");
        }

        userRepository.save(user);

        // 5. Generar el Token JWT Real
        String token = jwtUtil.generateToken(user.getEmail());

        // 6. Construir respuesta (Asegúrate que tu DTO AuthResponseDTO tenga este constructor)
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

    /**
     * Proceso de Registro de nuevos usuarios
     */
    @Transactional
    public UserEntity register(UserRegistrationDTO dto) {
        // 1. Validar si el email ya existe
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Este correo ya se encuentra registrado");
        }

        // 2. Crear nueva entidad
        UserEntity newUser = new UserEntity();
        newUser.setName(dto.getName());
        newUser.setEmail(dto.getEmail());
        newUser.setPhone(dto.getPhone());

        // 3. Encriptar contraseña antes de guardar
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. Valores por defecto
        newUser.setRole("USER");
        newUser.setLoginCount(0);
        newUser.setSubscriptionPlan("BASIC");

        return userRepository.save(newUser);
    }

    /**
     * Obtener datos de perfil
     */
    public UserDataDTO getUserById(String id) {
        // Manejamos el ID como Long o String según tu UserRepository
        return userRepository.findById(Long.parseLong(id))
                .map(user -> {
                    UserDataDTO dto = new UserDataDTO();
                    dto.setUserId(user.getId().toString());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setLoginCount(user.getLoginCount());
                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public void updatePersonType(String email, String personType) {
        // Buscamos al usuario por ID
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se pudo encontrar el usuario para actualizar el perfil"));

        // Asignamos el tipo (FISICA o MORAL)
        user.setPersonType(personType);

        // Guardamos los cambios
        userRepository.save(user);
    }


}