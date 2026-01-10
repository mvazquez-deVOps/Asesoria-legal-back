package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.*;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO credentials) {
        try {
            // El servicio ahora devolverá un TOKEN REAL
            AuthResponseDTO response = userService.authenticate(credentials);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado", "message", e.getMessage()));
        }
    }

    @PostMapping("/register") // <--- AÑADE ESTO
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO registration) {
        try {
            return ResponseEntity.ok(userService.register(registration));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Error en registro", "message", e.getMessage()));
        }
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<UserDataDTO> getUserProfile(@PathVariable String id) {
        UserDataDTO user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    @PutMapping("/update-person-type")
    public ResponseEntity<?> updatePersonType(@RequestBody Map<String, String> request, Principal principal) {
        String type = request.get("type");
        String email = principal.getName();

        userService.updatePersonType(email, type);
        return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
    }


}