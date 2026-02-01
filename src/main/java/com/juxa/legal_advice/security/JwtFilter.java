package com.juxa.legal_advice.security;

import com.juxa.legal_advice.security.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        // El @Lazy evita el ciclo de dependencias
        this.userDetailsService = userDetailsService;
    }

    @Override
        protected void doFilterInternal(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain filterChain)
                throws ServletException, IOException {
        String uri = request.getRequestURI();

        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) ||
                uri.contains("/api/ai/chat") ||
                uri.startsWith("/api/ai/generate-initial-diagnosis") ||
                uri.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }
        System.out.println("JwtFilter interceptando URI: " + request.getRequestURI());

        String authHeader = request.getHeader("Authorization");
        String email = null; // En JUXA usamos el email como identificador principal
        String token = null;

        // 1. Extraer el token del encabezado Bearer
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
                try {
                    // Extraemos el email (subject) del token usando JwtUtil
                    email = jwtUtil.extractUsername(token);
                } catch (Exception e) {
                    // Si el token es inválido o expiró, simplemente no autenticamos
                }
            }

            // 2. Validar el token y autenticar al usuario en el contexto de Spring
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                    if (jwtUtil.validateToken(token, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                } catch (UsernameNotFoundException e) {
                    // Si el usuario no existe en la BD, se ignora la autenticación
                }
            }

            // Continuar con el siguiente filtro o llegar al Controlador
            filterChain.doFilter(request, response);
        }
    }