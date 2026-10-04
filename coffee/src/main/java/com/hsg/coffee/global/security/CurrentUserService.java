package com.hsg.coffee.global.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CurrentUserService {

    private final AppUserRepository appUserRepository;

    public Optional<Long> getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        if (authentication.getPrincipal() instanceof BrewlogOAuth2User user) {
            return Optional.of(user.getAppUserId());
        }
        return Optional.empty();
    }

    public Optional<AppUser> getCurrentUser() {
        return getCurrentUserId().flatMap(appUserRepository::findById);
    }
}
