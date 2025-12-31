package com.juxa.legal_advice.controller;

import org.apache.catalina.connector.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@RequestBody AuthRequest credentials){
     //logica de autenticación
        return ResponseEntity.ok(new AuthResponseDTO("token123", "user123"));
    }
    @GetMapping("/profile/{id}")
    public ResponseEntity<UserDataDTO> getUserProfile(@PathVariable String id){
        // ógica para obtención del perfil
        return Response.Entity.ok(userService.getUserById(id));
    }
}
