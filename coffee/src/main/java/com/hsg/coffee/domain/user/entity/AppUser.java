package com.hsg.coffee.domain.user.entity;

import com.hsg.coffee.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "app_users",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_app_users_provider_user",
                columnNames = {"oauth_provider", "provider_user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppUser extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", nullable = false, length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "provider_user_id", nullable = false, length = 100)
    private String providerUserId;

    @Column(length = 100)
    private String nickname;

    @Column(length = 500)
    private String profileImageUrl;

    public static AppUser create(
            OAuthProvider oauthProvider,
            String providerUserId,
            String nickname,
            String profileImageUrl
    ) {
        AppUser user = new AppUser();
        user.oauthProvider = oauthProvider;
        user.providerUserId = providerUserId;
        user.updateProfile(nickname, profileImageUrl);
        return user;
    }

    public void updateProfile(String nickname, String profileImageUrl) {
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
    }
}
