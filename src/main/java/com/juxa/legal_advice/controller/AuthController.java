package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.*;
import com.juxa.legal_advice.dto.resetPassword.ForgotPasswordRequestDTO;
import com.juxa.legal_advice.dto.resetPassword.ResetPasswordRequestDTO;
import com.juxa.legal_advice.dto.resetPassword.VerifyOtpRequestDTO;
import com.juxa.legal_advice.service.AuthService;
import com.juxa.legal_advice.service.PasswordResetService;
import com.juxa.legal_advice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Slf4j // Agregamos Lombok para logs si los necesitas aquí
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final PasswordResetService passwordResetService;
    private final UserService userService;
    private final AuthService authService;
    @Value("${google.client.id}")
    private String googleClientId;


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

    @GetMapping("/confirm")
    public ResponseEntity<?> confirmUserAccount(@RequestParam("token") String token) {
        boolean isConfirmed = authService.confirmEmail(token);

        if (isConfirmed) {
            return ResponseEntity.ok(Map.of("message", "¡Cuenta confirmada exitosamente! Ya puedes iniciar sesión."));
            // Nota: Si tienes frontend, aquí podrías hacer un redirect (ej. RedirectView)
            // hacia tu página de Login en React/Angular/Vue en lugar de devolver un JSON.
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "El enlace es inválido o ha expirado."));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<AuthRegistrationResponseDTO> register(@RequestBody UserRegistrationDTO registration) {
        registration.setEmail(registration.getEmail().trim().toLowerCase());
        return ResponseEntity.ok(authService.register(registration));
    }

    @PutMapping("/update-person-type")
    public ResponseEntity<Map<String, String>> updatePersonType(@RequestBody Map<String, String> request, Principal principal) {
        userService.updatePersonType(principal.getName(), request.get("type"));
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

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        passwordResetService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok("Si el correo existe, se ha enviado un código de recuperación.");
    }

    // NUEVO ENDPOINT: Solo verifica el código
    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequestDTO request) {
        passwordResetService.verifyOtp(request.getEmail(), request.getOtp());
        return ResponseEntity.ok("Código verificado correctamente. Procede a ingresar tu nueva contraseña.");
    }

    // ENDPOINT MODIFICADO: Hace el cambio final
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        // El frontend debe enviar el email, el código (de nuevo) y la contraseña nueva
        passwordResetService.resetPassword(request.getEmail(), request.getOtp(), request.getNewPassword());
        return ResponseEntity.ok("Contraseña actualizada exitosamente.");
    }
}