package com.juxa.legal_advice.security;

import com.juxa.legal_advice.service.RateLimitingService;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Extraer la IP (soporta proxies/balanceadores si los agregas en el futuro)
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }

        Bucket tokenBucket = rateLimitingService.resolveBucket(ip);

        // Intenta consumir 1 token
        if (tokenBucket.tryConsume(1)) {
            // Hay tokens, la petición continúa
            filterChain.doFilter(request, response);
        } else {
            // No hay tokens, bloqueamos la petición
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Demasiadas peticiones. Intenta más tarde.\"}");
        }
    }
}