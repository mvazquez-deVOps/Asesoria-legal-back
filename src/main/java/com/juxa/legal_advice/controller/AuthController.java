package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.AuthRequestDTO;
import com.juxa.legal_advice.dto.AuthResponseDTO;
import com.juxa.legal_advice.dto.UserDataDTO;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Esto inyecta automáticamente el UserService
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO credentials) {
        try {
            // Llamamos al servicio que VALIDARÁ la contraseña
            AuthResponseDTO response = userService.authenticate(credentials);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            // AQUÍ ESTÁ EL ALERTAMIENTO: Si falla, mandamos 401 y el mensaje de error
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado", "message", e.getMessage()));
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserDataDTO> getUserProfile(@PathVariable String id) {
        UserDataDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
}
