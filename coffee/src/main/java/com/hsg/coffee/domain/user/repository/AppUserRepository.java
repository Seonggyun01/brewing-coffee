package com.hsg.coffee.domain.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.entity.OAuthProvider;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {

    Optional<AppUser> findByOauthProviderAndProviderUserId(OAuthProvider oauthProvider, String providerUserId);
}
