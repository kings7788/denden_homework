package com.bryant.denden_homework.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.ColumnDefault;

/**
 * A one-time token tied to a user, used for account activation (ACTIVATION)
 * or login second-factor (LOGIN_OTP). Has an expiry and is invalidated once used.
 */
@Entity
@Table(name = "verification_tokens")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    /** The OTP code (LOGIN_OTP only); null for ACTIVATION tokens. */
    @Column(name = "code")
    private String code;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used;

    /** Failed OTP attempts; the challenge is burned once this reaches the max (LOGIN_OTP only). */
    @ColumnDefault("0")
    @Column(nullable = false)
    private int attempts;
}
