package com.hsg.coffee.global.security;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.hsg.coffee.domain.user.entity.AppUser;

import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserControllerAdvice {

    private final CurrentUserService currentUserService;

    @ModelAttribute("signedInUser")
    public AppUser signedInUser() {
        return currentUserService.getCurrentUser().orElse(null);
    }
}
