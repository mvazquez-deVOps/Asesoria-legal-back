package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.config.exceptions.auth.InvalidCredentialsException;
import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserRegistrationDTO;
import com.juxa.legal_advice.security.JwtUtil;
import com.juxa.legal_advice.service.AuthService;
import com.juxa.legal_advice.service.RateLimitingService;
import com.juxa.legal_advice.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false) // Apagamos la seguridad para probar solo el controlador
public class AuthControllerUnitTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private RateLimitingService rateLimitingService;

    @Test
    void testLogin_Success_Returns200() throws Exception {
        AuthResponseDTO mockResponse = AuthResponseDTO.builder().token("token123").email("test@juxa.com").build();
        Mockito.when(userService.authenticate(any(AuthRequestDTO.class))).thenReturn(mockResponse);

        String payload = "{\"email\":\"test@juxa.com\", \"password\":\"12345\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token123"));
    }

    @Test
    void testLogin_InvalidCredentials_Returns401() throws Exception {
        // Simulamos que el servicio lanza nuestra nueva excepción personalizada
        Mockito.when(userService.authenticate(any(AuthRequestDTO.class)))
                .thenThrow(new InvalidCredentialsException("La contraseña es incorrecta."));

        String payload = "{\"email\":\"test@juxa.com\", \"password\":\"wrongpass\"}";

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized()) // 401
                .andExpect(jsonPath("$.error").value("Acceso Denegado"))
                .andExpect(jsonPath("$.message").value("La contraseña es incorrecta."));
    }

    @Test
    void testUpdatePersonType_Success() throws Exception {
        Principal mockPrincipal = () -> "usuario@juxa.com";
        String payload = "{\"type\":\"MORAL\"}";

        mockMvc.perform(put("/api/auth/update-person-type")
                        .principal(mockPrincipal)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"));

        Mockito.verify(userService).updatePersonType("usuario@juxa.com", "MORAL");
    }
}