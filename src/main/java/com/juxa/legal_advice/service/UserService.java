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
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO credentials) {
        UserEntity user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new RuntimeException("Usuario no existe"));

        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new RuntimeException("Contraseña incorrecta");
        }

        user.setLoginCount((user.getLoginCount() != null ? user.getLoginCount() : 0) + 1);
        userRepository.save(user);

        return new AuthResponseDTO("token-v101", user.getId().toString());
    }

    public UserDataDTO getUserById(String id) {
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