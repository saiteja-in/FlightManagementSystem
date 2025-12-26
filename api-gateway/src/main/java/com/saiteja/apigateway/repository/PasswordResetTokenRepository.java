package com.saiteja.apigateway.repository;

import com.saiteja.apigateway.model.PasswordResetToken;
import com.saiteja.apigateway.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    @Query("SELECT prt FROM PasswordResetToken prt JOIN FETCH prt.user WHERE prt.token = :token")
    Optional<PasswordResetToken> findByToken(@Param("token") String token);

    Optional<PasswordResetToken> findByUser(User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.user = :user")
    void deleteByUser(@Param("user") User user);

    @Modifying
    @Query("DELETE FROM PasswordResetToken prt WHERE prt.expiryDate < :now")
    void deleteByExpiryDateBefore(@Param("now") LocalDateTime now);
}

