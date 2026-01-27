package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.*;
import com.juxa.legal_advice.service.AuthService;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor // Inyecta automáticamente userService y authService
public class AuthController {

    private final UserService userService;
    private final AuthService authService; // <--- AGREGA ESTA LÍNEA PARA QUITAR EL ROJO
    @Value("${google.client.id}")
    private String googleClientId;

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> request) {
        String idTokenString = request.get("token"); // El token que envía el Front
        try {
            // Este método validará el token y creará/logueará al usuario
            AuthResponseDTO response = authService.verifyGoogleToken(idTokenString);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token de Google inválido", "message", e.getMessage()));
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequestDTO credentials) {
        try {
            AuthResponseDTO response = userService.authenticate(credentials);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Acceso denegado", "message", e.getMessage()));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserRegistrationDTO registration) {
        try {
            // Ahora 'authService' ya no marcará error porque está declarado arriba
            AuthResponseDTO response = authService.register(registration);
            return ResponseEntity.ok(response);
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
    @PutMapping("/update-person-type/{id}")
    public ResponseEntity<?> updatePersonType(@PathVariable String id, @RequestBody Map<String, String> body) {
        // En el Front me mostraste que envías "personType"
        String type = body.get("personType");

        try {
            // Delegamos la lógica al servicio para que el controlador esté limpio
            userService.updatePersonTypeById(id, type);
            return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "No se pudo actualizar", "message", e.getMessage()));
        }
    }



}