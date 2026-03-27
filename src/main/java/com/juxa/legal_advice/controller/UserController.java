package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.UserSubscriptionResponseDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.service.UsageAuthorizationService;
import com.juxa.legal_advice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private UsageAuthorizationService usageAuthService;


    @GetMapping("/me/subscription")
    public ResponseEntity<UserSubscriptionResponseDTO> getMySubscription(Principal principal) {
        // 'principal.getName()' nos dará el email del usuario que viene en el token JWT


        String email = principal.getName();

        UserSubscriptionResponseDTO response = userService.getMySubscriptionStatus(email);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAccount(@PathVariable("id") Long id) {
        try {
            userService.deleteUserCompletely(id);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Cuenta y datos asociados eliminados permanentemente");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("message", "Error al procesar la eliminación de la cuenta: " + e.getMessage());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }
}