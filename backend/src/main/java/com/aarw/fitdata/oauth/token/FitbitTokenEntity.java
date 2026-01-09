package com.aarw.fitdata.oauth.token;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "fitbit_token")
public class FitbitTokenEntity {

    @Getter
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Getter
    @Column(name = "fitbit_user_id", nullable = false, unique = true, length = 64)
    private String fitbitUserId;

    @Setter
    @Getter
    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    private String accessToken;

    @Setter
    @Getter
    @Column(name = "refresh_token", nullable = false, columnDefinition = "text")
    private String refreshToken;

    @Setter
    @Getter
    @Column(name = "scope", columnDefinition = "text")
    private String scope;

    @Getter
    @Setter
    @Column(name = "token_type", length = 32)
    private String tokenType;

    @Getter
    @Setter
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Getter
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }

}