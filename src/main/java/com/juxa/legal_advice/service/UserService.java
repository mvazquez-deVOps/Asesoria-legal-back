package com.juxa.legal_advice.service;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor // Inyecta el UserRepository automáticamente
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO credentials) {
        // 1. Buscamos al usuario en la nueva tabla 'users'
        UserEntity user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new RuntimeException("Credenciales incorrectas: El correo no existe."));

        // 2. Validación Real con BCrypt
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new RuntimeException("Credenciales incorrectas: Contraseña fallida.");
        }

        // 3. Incremento del contador de ingresos (Paso 2 de tu plan)
        int currentCount = (user.getLoginCount() != null) ? user.getLoginCount() : 0;
        user.setLoginCount(currentCount + 1);

        // Guardamos los cambios (esto actualiza la fila existente)
        userRepository.save(user);

        // 4. Retornamos la respuesta (Aquí generarías tu JWT real)
        return new AuthResponseDTO("TOKEN_PROVISIONAL_101", String.valueOf(user.getId()));
    }

    // Método para obtener los datos del perfil en el Dashboard
    public UserDataDTO getUserById(String id) { // <--- Asegúrate que diga String aquí
        return userRepository.findById(Long.parseLong(id))
                .map(user -> {
                    UserDataDTO dto = new UserDataDTO();
                    dto.setUserId(user.getId().toString());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
}