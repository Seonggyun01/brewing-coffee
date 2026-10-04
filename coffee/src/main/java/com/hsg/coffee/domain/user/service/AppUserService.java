package com.hsg.coffee.domain.user.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hsg.coffee.domain.coffeeBean.repository.CoffeeBeanRepository;
import com.hsg.coffee.domain.purchasePlace.repository.PurchasePlaceRepository;
import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.entity.OAuthProvider;
import com.hsg.coffee.domain.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final PurchasePlaceRepository purchasePlaceRepository;
    private final CoffeeBeanRepository coffeeBeanRepository;

    @Transactional
    public synchronized AppUser loginWithKakao(
            String providerUserId,
            String nickname,
            String profileImageUrl
    ) {
        AppUser user = appUserRepository.findByOauthProviderAndProviderUserId(
                        OAuthProvider.KAKAO,
                        providerUserId
                )
                .orElseGet(() -> appUserRepository.save(AppUser.create(
                        OAuthProvider.KAKAO,
                        providerUserId,
                        nickname,
                        profileImageUrl
                )));
        user.updateProfile(nickname, profileImageUrl);

        if (appUserRepository.count() == 1) {
            purchasePlaceRepository.assignOwnerToUnowned(user);
            coffeeBeanRepository.assignOwnerToUnowned(user);
        }
        return user;
    }
}
