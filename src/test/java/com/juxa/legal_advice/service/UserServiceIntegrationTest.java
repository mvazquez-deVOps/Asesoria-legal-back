package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.exceptions.auth.InvalidCredentialsException;
import com.juxa.legal_advice.config.exceptions.auth.ResourceNotFoundException;
import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Limpia la BD después de cada test
public class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        UserEntity user = new UserEntity();
        user.setEmail("normal@gmail.com");
        user.setPassword(passwordEncoder.encode("password123")); // Contraseña encriptada
        user.setName("Usuario Normal");
        user.setLoginCount(0);
        user.setRole("USER");
        userRepository.save(user);
    }

    @Test
    void authenticate_ValidCredentials_ReturnsTokenAndIncrementsLogin() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("normal@gmail.com");
        request.setPassword("password123");

        AuthResponseDTO response = userService.authenticate(request);

        assertNotNull(response.getToken());
        assertEquals("normal@gmail.com", response.getEmail());

        // Verificamos que el contador de logins subió
        UserEntity updatedUser = userRepository.findByEmail("normal@gmail.com").get();
        assertEquals(1, updatedUser.getLoginCount());
    }

    @Test
    void authenticate_AdminDomain_AssignsPremiumAndAdminRole() {
        // Creamos un usuario con dominio juxa.mx
        UserEntity admin = new UserEntity();
        admin.setEmail("admin@juxa.mx");
        admin.setPassword(passwordEncoder.encode("adminpass"));
        admin.setRole("USER");
        userRepository.save(admin);

        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("admin@juxa.mx");
        request.setPassword("adminpass");

        AuthResponseDTO response = userService.authenticate(request);

        assertEquals("ADMIN", response.getRole());
        assertEquals("PREMIUM", response.getSubscriptionPlan());
    }

    @Test
    void authenticate_WrongPassword_ThrowsInvalidCredentialsException() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("normal@gmail.com");
        request.setPassword("contraseña_equivocada");

        assertThrows(InvalidCredentialsException.class, () -> {
            userService.authenticate(request);
        });
    }

    @Test
    void authenticate_UnknownEmail_ThrowsResourceNotFoundException() {
        AuthRequestDTO request = new AuthRequestDTO();
        request.setEmail("fantasma@gmail.com");
        request.setPassword("12345");

        assertThrows(ResourceNotFoundException.class, () -> {
            userService.authenticate(request);
        });
    }
}