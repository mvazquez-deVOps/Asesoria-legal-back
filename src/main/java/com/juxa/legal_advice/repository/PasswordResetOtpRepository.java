package com.juxa.legal_advice.repository;

import com.juxa.legal_advice.model.PasswordResetOtp;
import com.juxa.legal_advice.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PasswordResetOtpRepository extends JpaRepository<PasswordResetOtp, Long> {
    Optional<PasswordResetOtp> findByOtpAndUser(String otp, UserEntity user);
    void deleteByUser(UserEntity user); // Útil para borrar OTPs viejos si el usuario pide uno nuevo
}