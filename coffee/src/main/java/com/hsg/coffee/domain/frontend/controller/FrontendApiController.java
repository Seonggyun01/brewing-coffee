package com.hsg.coffee.domain.frontend.controller;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import com.hsg.coffee.domain.coffeeBean.dto.*;
import com.hsg.coffee.domain.coffeeBean.service.*;
import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.cafeFilterCoffee.dto.CafeFilterCoffeeForm;
import com.hsg.coffee.domain.cafeFilterCoffee.service.CafeFilterCoffeeService;
import com.hsg.coffee.global.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** JSON transport for the Nuxt frontend; business rules stay in existing services. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/front")
public class FrontendApiController {
    private final CoffeeBeanService beans;
    private final BrewRecordService brews;
    private final CafeFilterCoffeeService cafes;
    private final CoffeeBeanCardExtractionService extraction;
    private final CoffeeBeanCardImagePreprocessor imagePreprocessor;
    private final CoffeeBeanCardExtractionProgressTracker progress;
    private final CurrentUserService currentUser;
    @Value("${brewlog.kakao.javascript-key:}") private String kakaoKey;
    @Value("${brewlog.security.enabled:true}") private boolean securityEnabled;

    @GetMapping("/session")
    public Map<String, Object> session(CsrfToken csrf) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", currentUser.getCurrentUser().map(u -> Map.of("id", u.getId(), "nickname", u.getNickname() != null ? u.getNickname() : "커피 애호가")).orElse(null));
        result.put("securityEnabled", securityEnabled);
        result.put("kakaoJavascriptKey", kakaoKey);
        if (csrf != null) result.put("csrf", Map.of("token", csrf.getToken(), "headerName", csrf.getHeaderName()));
        return result;
    }
    @GetMapping("/beans") public Object beans(@RequestParam(required=false) String keyword, @RequestParam(required=false) String countryCode) { return beans.findCoffeeBeans(keyword, countryCode); }
    @GetMapping("/beans/{id}") public Object bean(@PathVariable Long id) { return beans.get(id); }
    @GetMapping("/beans/{id}/form") public Object beanForm(@PathVariable Long id) { return beans.getUpdateForm(id); }
    @PostMapping("/beans") public Object createBean(@Valid @RequestBody CoffeeBeanCreateForm form) { return Map.of("id", beans.create(form)); }
    @PutMapping("/beans/{id}") public void updateBean(@PathVariable Long id, @Valid @RequestBody CoffeeBeanUpdateForm form) { beans.update(id, form); }
    @DeleteMapping("/beans/{id}") public void deleteBean(@PathVariable Long id) { beans.delete(id); }
    @PostMapping("/image-preview")
    public org.springframework.http.ResponseEntity<byte[]> imagePreview(@RequestParam MultipartFile image) {
        String type = image.getContentType();
        if (image.isEmpty() || image.getSize() > 5 * 1024 * 1024
                || !java.util.Set.of("image/heic", "image/heif", "image/jpeg", "image/png", "image/webp").contains(type == null ? "" : type)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "지원하지 않는 이미지입니다.");
        }
        var prepared = imagePreprocessor.prepare(image);
        return org.springframework.http.ResponseEntity.ok()
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .contentType(org.springframework.http.MediaType.parseMediaType(prepared.contentType()))
                .body(prepared.bytes());
    }
    @PostMapping("/extraction") public Object extract(@RequestParam MultipartFile image, @RequestParam String extractionProgressId) {
        String traceId = extractionProgressId.matches("[A-Za-z0-9_-]{1,100}") ? extractionProgressId : java.util.UUID.randomUUID().toString();
        try (var ignored = org.slf4j.MDC.putCloseable(com.hsg.coffee.global.logging.ExtractionTrace.REQUEST_ID, traceId)) {
            var result = extraction.extract(image, phase -> progress.update(extractionProgressId, phase));
            progress.update(extractionProgressId, CoffeeBeanCardExtractionPhase.COMPLETED);
            // Purchase date and status remain deliberate user input.
            result.getForm().setPurchasedDate(null);
            return result;
        } catch (IllegalArgumentException exception) {
            progress.update(extractionProgressId, CoffeeBeanCardExtractionPhase.FAILED);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage());
        }
    }
    @GetMapping("/brews") public Object brews() { return brews.getAll(); }
    @GetMapping("/brews/{id}") public Object brew(@PathVariable Long id) { return brews.get(id); }
    @GetMapping("/brews/{id}/form") public Object brewForm(@PathVariable Long id) { return brews.getUpdateForm(id); }
    @PostMapping("/brews") public Object createBrew(@Valid @RequestBody BrewRecordForm form) { return Map.of("id", brews.create(form)); }
    @PutMapping("/brews/{id}") public void updateBrew(@PathVariable Long id, @Valid @RequestBody BrewRecordForm form) { brews.update(id, form); }
    @DeleteMapping("/brews/{id}") public void deleteBrew(@PathVariable Long id) { brews.delete(id); }
    @GetMapping("/cafes") public Object cafes(@RequestParam(required=false) Long cafeId, @RequestParam(required=false) String q) { return cafes.getByCafeAndQuery(cafeId, q); }
    @GetMapping("/cafes/{id}") public Object cafe(@PathVariable Long id) { return cafes.get(id); }
    @GetMapping("/cafes/{id}/form") public Object cafeForm(@PathVariable Long id) { return cafes.getUpdateForm(id); }
    @PostMapping("/cafes") public Object createCafe(@Valid @RequestBody CafeFilterCoffeeForm form) { form.setRecordRecipe(false); return Map.of("id", cafes.create(form)); }
    @PutMapping("/cafes/{id}") public void updateCafe(@PathVariable Long id, @Valid @RequestBody CafeFilterCoffeeForm form) { form.setRecordRecipe(false); cafes.update(id, form); }
    @DeleteMapping("/cafes/{id}") public void deleteCafe(@PathVariable Long id) { cafes.delete(id); }
}
