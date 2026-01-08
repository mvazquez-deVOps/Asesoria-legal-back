package com.juxa.legal_advice.service;

import org.springframework.stereotype.Service;

@Service
public class JwtService {
    public String generateToken(String email) {
        return "token-provisional-v101";
    }
}
