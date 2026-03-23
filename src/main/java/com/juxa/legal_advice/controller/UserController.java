package com.juxa.legal_advice.controller;

import com.juxa.legal_advice.dto.UserSubscriptionResponseDTO;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.service.UsageAuthorizationService;
import com.juxa.legal_advice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

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
        UserEntity currentUser = userService.getCurrentAuthenticatedUser();
        usageAuthService.authorizeAndConsumeQuery(currentUser);

        UserSubscriptionResponseDTO response = userService.getMySubscriptionStatus(email);

        return ResponseEntity.ok(response);
    }
}