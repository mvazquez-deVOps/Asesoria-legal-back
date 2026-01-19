package com.juxa.legal_advice.config; //

import com.juxa.legal_advice.repository.UserRepository; // Tu repositorio de Legal
import com.juxa.legal_advice.security.JwtFilter;       // Tu filtro de Legal
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    // 1. Cargar usuarios desde tu tabla de usuarios legal
    @Bean
    public UserDetailsService userDetailsService(UserRepository userRepository) {
        return username -> {
            // Usamos orElseThrow para manejar el Optional correctamente
            var user = userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

            return org.springframework.security.core.userdetails.User
                    .builder()
                    .username(user.getEmail())
                    .password(user.getPassword())
                    .roles(user.getRole()) // Ahora toma 'ADMIN' o 'USER' de la BD automáticamente
                    .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider(UserRepository userRepository) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService(userRepository));
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    // 2. Configurar los permisos de las rutas legales
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sess -> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 1. AJUSTE: Permitir explícitamente los OPTIONS (Preflight)
                        // Esto evita que el navegador bloquee el POST antes de que ocurra
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // 2. AJUSTE: Usar comodín ** para la autenticación
                        // Esto evita el 403 si el Front envía una diagonal extra al final
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/ai/generate-initial-diagnosis").permitAll()

                        .requestMatchers("/api/ai/chat").authenticated()
                        .requestMatchers("/api/diagnoses/**").authenticated()
                        .requestMatchers("/api/pdf/**").authenticated()
                        .requestMatchers("/api/auth/update-person-type/**").permitAll()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration = new org.springframework.web.cors.CorsConfiguration();

        configuration.setAllowedOrigins(java.util.Arrays.asList(
                "https://asesoria-legal-juxa-83a12.web.app",
                "https://asesoria-legal-juxa-83a12.firebaseapp.com",
                "http://localhost:3000",
                "http://localhost:5173",
                "http://172.26.16.1:3000",
                "http://192.168.11.235:3000"
        ));

        configuration.setAllowedMethods(java.util.Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // Permite * cualquier encabezado que envíe el navegador
        configuration.setAllowedHeaders(java.util.Arrays.asList("*"));

        configuration.setAllowCredentials(true);
        // Exponemos el encabezado de Authorization por si el Front necesita leerlo
        configuration.setExposedHeaders(java.util.Arrays.asList("Authorization"));

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source = new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

}