package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.*;
import com.juxa.legal_advice.service.AuthService;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Slf4j // Agregamos Lombok para logs si los necesitas aquí
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;

    @PostMapping("/google")
    public ResponseEntity<AuthResponseDTO> googleLogin(@RequestBody Map<String, String> request) throws Exception {
        String idTokenString = request.get("token");
        // Si falla, el servicio lanza InvalidCredentialsException y el GlobalHandler responde 401
        return ResponseEntity.ok(authService.verifyGoogleToken(idTokenString));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequestDTO credentials) {
        return ResponseEntity.ok(userService.authenticate(credentials));
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponseDTO> register(@RequestBody UserRegistrationDTO registration) {
        registration.setEmail(registration.getEmail().trim().toLowerCase());
        return ResponseEntity.ok(authService.register(registration));
    }

    @PutMapping("/update-person-type")
    public ResponseEntity<Map<String, String>> updatePersonType(@RequestBody Map<String, String> request, Principal principal) {
        userService.updatePersonType(principal.getName(), request.get("type"));
        return ResponseEntity.ok(Map.of("message", "Perfil actualizado correctamente"));
    }
}



        /*  Endpoint no usado actualmente.

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
    */