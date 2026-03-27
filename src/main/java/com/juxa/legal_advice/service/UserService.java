package com.juxa.legal_advice.service;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.config.exceptions.UnauthorizedUserException;
import com.juxa.legal_advice.dto.*;
import com.juxa.legal_advice.model.PlanEntity;
import com.juxa.legal_advice.model.SubscriptionEntity;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.repository.SubscriptionRepository;
import com.juxa.legal_advice.repository.UserRepository;
import com.juxa.legal_advice.security.JwtUtil;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder; // Inyectado desde SecurityConfig
    private final SubscriptionRepository subscriptionRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    /**
     * Proceso de Login y Autenticación
     */

    @Transactional
    public void deactivateExpiredActiveSubscriptions() {
        try {
            // 1. PRIMERO actualizamos los usuarios a FREE
            int updatedUsers = userRepository.updateExpiredSubscriptionsToFree();

            // 2. DESPUÉS marcamos las suscripciones como inyectivas
            int updatedSubs = subscriptionRepository.updateExpiredSubscriptionsStatus();

            logger.info("Grace period ended: Updated {} users to FREE and marked {} subscriptions as inactive.", updatedUsers, updatedSubs);
        } catch (Exception e) {
            logger.error("Error updating active subscriptions past grace period: ", e);
        }
    }

    @Transactional
    public void deactivateExpiredTrialSubscriptions() {
        try {
            // 1. PRIMERO actualizamos los usuarios de trial a FREE
            int updatedUsers = userRepository.updateExpiredTrialingUsersToFree();

            // 2. DESPUÉS marcamos las suscripciones de trial como inyectivas
            int updatedSubs = subscriptionRepository.updateExpiredTrialingSubscriptionsStatus();

            logger.info("Trials ended: Updated {} users to FREE and marked {} trial subscriptions as inactive.", updatedUsers, updatedSubs);
        } catch (Exception e) {
            logger.error("Error updating expired trial subscriptions: ", e);
        }
    }

    @Transactional
    public AuthResponseDTO authenticate(AuthRequestDTO credentials) {
        // 1. Buscar usuario por email
        UserEntity user = userRepository.findByEmail(credentials.getEmail())
                .orElseThrow(() -> new RuntimeException("El correo electrónico no está registrado"));

        // 2. Validar contraseña con BCrypt
        if (!passwordEncoder.matches(credentials.getPassword(), user.getPassword())) {
            throw new RuntimeException("La contraseña es incorrecta");
        }

        // 3. Actualizar estadísticas de sesión
        user.setLoginCount((user.getLoginCount() == null ? 0 : user.getLoginCount()) + 1);

        // 4. Lógica de privilegios para dominio JUXA
        if (user.getEmail().toLowerCase().endsWith("@juxa.mx")) {
            user.setSubscriptionPlan("PREMIUM");
            user.setRole("ADMIN");
        }

        userRepository.save(user);

        // 5. Generar el Token JWT Real
        String token = jwtUtil.generateToken(user.getEmail());

        // 6. Construir respuesta (Asegúrate que tu DTO AuthResponseDTO tenga este constructor)
        return AuthResponseDTO.builder()
                .token(token)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .name(user.getName())
                .loginCount(user.getLoginCount())
                .role(user.getRole())
                .subscriptionPlan(user.getSubscriptionPlan())
                .personType(user.getPersonType())
                .phone(user.getPhone()) // si lo tienes en la entidad
                .build();

    }

    public UserSubscriptionResponseDTO getMySubscriptionStatus(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        String planName = user.getSubscriptionPlan() != null ? user.getSubscriptionPlan() : "FREE";

        // ========================================================================
        // Si es FREE, no buscamos en la tabla Subscriptions
        // ========================================================================
        if ("FREE".equalsIgnoreCase(planName) || "BASIC".equalsIgnoreCase(planName)) {
            boolean isTrialActive = user.getTrialEndDate() != null && user.getTrialEndDate().isAfter(LocalDateTime.now());

            return UserSubscriptionResponseDTO.builder()
                    .hasActiveSubscription(isTrialActive)
                    .planName("FREE")
                    .status(isTrialActive ? "trialing" : "inactive")
                    .currentPeriodEnd(user.getTrialEndDate())
                    .willCancelAtPeriodEnd(false)
                    .build();
        }
        // ========================================================================
        // Si NO es FREE, buscamos su suscripción de pago
        Optional<SubscriptionEntity> subOpt = subscriptionRepository.findByUserId(user.getId());

        if (subOpt.isPresent()) {
            SubscriptionEntity sub = subOpt.get();

// El CRON job ya se encarga de cambiar el status a 'inactive' en la BD
            // Por lo tanto, si el status es 'active' o 'trialing', la suscripción es válida.
            boolean isActive = "active".equalsIgnoreCase(sub.getStatus()) ||
                    "trialing".equalsIgnoreCase(sub.getStatus());

            boolean willCancel = sub.isCancelAtPeriodEnd();

            return UserSubscriptionResponseDTO.builder()
                    .hasActiveSubscription(isActive)
                    .planName(planName)
                    .status(sub.getStatus())
                    .currentPeriodEnd(sub.getCurrentPeriodEnd())
                    .willCancelAtPeriodEnd(willCancel)
                    .build();
        } else {
            logger.error("Base de datos desincronizada. Verificar si está conectado a  Stripe");
            // Fallback por si la base de datos se desincroniza (dice que tiene plan pero no hay registro)
            return UserSubscriptionResponseDTO.builder()
                    .hasActiveSubscription(false)
                    .planName("FREE")
                    .status("inactive")
                    .currentPeriodEnd(null)
                    .willCancelAtPeriodEnd(false)
                    .build();
        }
    }    /**
     * Proceso de Registro de nuevos usuarios
     */
    @Transactional
    public UserEntity register(UserRegistrationDTO dto) {
        // 1. Validar si el email ya existe
        if (userRepository.findByEmail(dto.getEmail()).isPresent()) {
            throw new RuntimeException("Este correo ya se encuentra registrado");
        }

        // 2. Crear nueva entidad
        UserEntity newUser = new UserEntity();
        newUser.setName(dto.getName());
        newUser.setEmail(dto.getEmail());
        newUser.setPhone(dto.getPhone());

        // 3. Encriptar contraseña antes de guardar
        newUser.setPassword(passwordEncoder.encode(dto.getPassword()));

        // 4. Valores por defecto
        newUser.setRole("USER");
        newUser.setLoginCount(0);
        newUser.setSubscriptionPlan("BASIC");

        return userRepository.save(newUser);
    }

    /**
     * Obtener datos de perfil
     */
    public UserDataDTO getUserById(String id) {
        // Manejamos el ID como Long o String según tu UserRepository
        return userRepository.findById(Long.parseLong(id))
                .map(user -> {
                    UserDataDTO dto = new UserDataDTO();
                    dto.setUserId(user.getId().toString());
                    dto.setName(user.getName());
                    dto.setEmail(user.getEmail());
                    dto.setLoginCount(user.getLoginCount());
                    return dto;
                })
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado con ID: " + id));
    }

    @Transactional
    public void updatePersonType(String email, String personType) {
        // Buscamos al usuario por ID
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No se pudo encontrar el usuario para actualizar el perfil"));

        // Asignamos el tipo (FISICA o MORAL)
        user.setPersonType(personType);

        // Guardamos los cambios
        userRepository.save(user);
    }
    public void updatePersonTypeById(String id, String type) {
        // Convertimos el String ID a Long para buscar en la base de datos
        Long userId = Long.parseLong(id);

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        user.setPersonType(type); // Seteamos "FISICA" o "MORAL"
        userRepository.save(user); // Guardamos los cambios
    }

    public UserEntity getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // Si no hay contexto o es un endpoint público (anónimo)
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new UnauthorizedUserException("No hay una sesión activa o el token es inválido.");
        }

        String email;
        Object principal = authentication.getPrincipal();

        // Dependiendo de cómo configuraste tu UserInfoDetails / JwtFilter
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario autenticado no encontrado en la base de datos."));
    }

    // -- Eliminar usuarios de la base de datos y de Stripe --
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserCompletely(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en la base de datos"));

        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isEmpty()) {
            try {
                Customer stripeCustomer = Customer.retrieve(user.getStripeCustomerId());
                stripeCustomer.delete();
            } catch (StripeException e) {
                System.err.println("Advertencia al borrar en Stripe: " + e.getMessage());
            }
        }
        userRepository.delete(user);
    }
}