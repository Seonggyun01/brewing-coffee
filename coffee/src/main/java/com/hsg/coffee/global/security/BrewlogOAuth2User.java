package com.hsg.coffee.global.security;

import java.io.Serial;
import java.util.Collection;
import java.util.Map;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

public class BrewlogOAuth2User extends DefaultOAuth2User {

    @Serial
    private static final long serialVersionUID = 1L;

    private final Long appUserId;
    private final String nickname;

    public BrewlogOAuth2User(
            Collection<? extends GrantedAuthority> authorities,
            Map<String, Object> attributes,
            String nameAttributeKey,
            Long appUserId,
            String nickname
    ) {
        super(authorities, attributes, nameAttributeKey);
        this.appUserId = appUserId;
        this.nickname = nickname;
    }

    public Long getAppUserId() {
        return appUserId;
    }

    public String getNickname() {
        return nickname;
    }
}
