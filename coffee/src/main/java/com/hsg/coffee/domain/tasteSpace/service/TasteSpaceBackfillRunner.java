package com.hsg.coffee.domain.tasteSpace.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.hsg.coffee.domain.user.entity.AppUser;
import com.hsg.coffee.domain.user.repository.AppUserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "brewlog.taste-space.backfill-enabled", havingValue = "true")
public class TasteSpaceBackfillRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TasteSpaceBackfillRunner.class);

    private final AppUserRepository appUserRepository;
    private final TasteSpaceService tasteSpaceService;

    @Override
    public void run(ApplicationArguments args) {
        for (AppUser user : appUserRepository.findAll()) {
            try {
                var result = tasteSpaceService.rebuildForOwner(user.getId());
                log.info(
                        "원두 취향 공간 백필 완료: userId={}, beans={}, state={}",
                        user.getId(),
                        result.beanCount(),
                        result.state()
                );
            } catch (RuntimeException exception) {
                log.error("원두 취향 공간 백필 실패: userId={}, reason={}", user.getId(), exception.getMessage(), exception);
            }
        }
    }
}
