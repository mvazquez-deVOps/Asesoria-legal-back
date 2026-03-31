package com.juxa.legal_advice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.UserRegistrationDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // Limpia la BD al final
public class AuthE2ETest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper; // Para convertir DTOs a JSON fácilmente

    @Test
    void endToEnd_RegisterAndLoginFlow() throws Exception {
        // 1. DTO de Registro
        UserRegistrationDTO registerDto = new UserRegistrationDTO();
        registerDto.setName("Nuevo E2E");
        registerDto.setEmail("nuevo_e2e@correo.com");
        registerDto.setPassword("Secreta123");
        registerDto.setPhone("5551234567");

        // 2. Ejecutar Registro
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.personType").value("FISICA"))
                .andExpect(jsonPath("$.subscriptionPlan").value("FREE"));

        // 3. DTO de Login (Usando las credenciales recién creadas)
        AuthRequestDTO loginDto = new AuthRequestDTO();
        loginDto.setEmail("nuevo_e2e@correo.com");
        loginDto.setPassword("Secreta123");

        // 4. Ejecutar Login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.loginCount").value(1)); // 0 en registro + 1 en login
    }

    @Test
    @WithMockUser(username = "logeado@juxa.com")
    void endToEnd_UpdatePersonType() throws Exception {
        // NOTA: Para un E2E estricto de update, deberíamos crear el usuario "logeado@juxa.com" en BD primero.
        // Pero como el servicio busca al usuario en la BD, lo inyectaremos rápidamente a través de la API de registro

        UserRegistrationDTO registerDto = new UserRegistrationDTO();
        registerDto.setEmail("logeado@juxa.com");
        registerDto.setPassword("pass");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDto)));

        // Ahora probamos la actualización (Asumiendo que el JWT nos identificó como "logeado@juxa.com")
        String payload = "{\"type\":\"MORAL\"}";

        mockMvc.perform(put("/api/auth/update-person-type")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"));
    }
}