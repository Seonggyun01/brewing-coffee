package com.hsg.coffee.global.security;

import java.util.Map;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.service.AppUserService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class KakaoOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AppUserService appUserService;
    private final DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = delegate.loadUser(userRequest);
        Map<String, Object> attributes = oauthUser.getAttributes();
        String providerUserId = String.valueOf(attributes.get("id"));
        Map<String, Object> properties = nestedMap(attributes.get("properties"));

        String nickname = text(properties.get("nickname"), "카카오 사용자");
        String profileImageUrl = text(properties.get("profile_image"), null);
        AppUser appUser = appUserService.loginWithKakao(providerUserId, nickname, profileImageUrl);

        return new BrewlogOAuth2User(
                oauthUser.getAuthorities(),
                attributes,
                "id",
                appUser.getId(),
                appUser.getNickname()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> nestedMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private String text(Object value, String fallback) {
        return value != null && !value.toString().isBlank() ? value.toString() : fallback;
    }
}
