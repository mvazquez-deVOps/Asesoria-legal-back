package com.juxa.legal_advice.security;

import io.jsonwebtoken.security.Keys;
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
import java.security.Key;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    private final Key secretKey = Keys.hmacShaKeyFor("claveSuperSecretaDe256bits1234567890123456".getBytes());

    @Autowired
    public JwtFilter(JwtUtil jwtUtil, @Lazy UserDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        // El @Lazy es fundamental aquí para evitar el error de ciclo de dependencias
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        String email = null; // En JUXA usamos el email como identificador principal
        String token = null;

        // 1. Extraer el token del encabezado Bearer
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);
            try {
                // Extraemos el email (subject) del token usando tu clase JwtUtil
                email = jwtUtil.extractUsername(token);
            } catch (Exception e) {
                // Si el token es inválido o expiró, simplemente no autenticamos
            }
        }

        // 2. Validar el token y autenticar al usuario en el contexto de Spring
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                // Buscamos los detalles del usuario en la base de datos de JUXA
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // Validamos que el token coincida con el usuario y no esté expirado
                if (jwtUtil.validateToken(token, userDetails.getUsername())) {
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Inyectamos al usuario autenticado en el sistema para esta petición
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