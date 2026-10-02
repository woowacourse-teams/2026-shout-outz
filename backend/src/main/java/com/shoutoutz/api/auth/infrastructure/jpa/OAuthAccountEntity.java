package com.shoutoutz.api.auth.infrastructure.jpa;

import com.shoutoutz.api.auth.domain.OAuthProvider;
import com.shoutoutz.api.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "oauth_accounts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OAuthAccountEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuthProvider provider;

    @Column(name = "provider_account_id", nullable = false, length = 255)
    private String providerAccountId;

    @Column(name = "provider_avatar_url")
    private String providerAvatarUrl;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;
}
