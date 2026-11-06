package com.project.rentmebarwaha.user.entity;

import com.project.rentmebarwaha.common.model.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseAuditEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(length = 150)
    private String name;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "google_id", length = 255)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private Role role;

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(length = 100)
    private String city;

    @Column(name = "kyc_verified", nullable = false)
    @Builder.Default
    private boolean kycVerified = false;

    @Column(name = "owner_verified", nullable = false)
    @Builder.Default
    private boolean ownerVerified = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @PrePersist
    @Override
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.city == null) {
            this.city = "Barwaha";
        }
        super.onCreate();
    }
}

