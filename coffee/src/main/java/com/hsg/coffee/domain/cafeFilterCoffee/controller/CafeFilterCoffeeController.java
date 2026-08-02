package com.hsg.coffee.domain.cafeFilterCoffee.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCardExtractResult;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanCardExtractionService;
import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.BrewTemperatureType;
import com.hsg.coffee.domain.brewRecord.entity.FlavorCategory;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.cafeFilterCoffee.dto.CafeFilterCoffeeForm;
import com.hsg.coffee.domain.cafeFilterCoffee.service.CafeFilterCoffeeService;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.global.country.CountryInfo;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cafe-filter-coffees")
public class CafeFilterCoffeeController {

    private final CafeFilterCoffeeService cafeFilterCoffeeService;
    private final CoffeeBeanCardExtractionService coffeeBeanCardExtractionService;

    @Value("${brewlog.kakao.javascript-key:}")
    private String kakaoJavascriptKey;

    @GetMapping
    public String list(
            @RequestParam(required = false) Long cafeId,
            @RequestParam(required = false) String q,
            Model model
    ) {
        model.addAttribute("cafeFilterCoffees", cafeFilterCoffeeService.getByCafeAndQuery(cafeId, q));
        model.addAttribute("filterCafes", cafeFilterCoffeeService.getFilterCafes());
        model.addAttribute("selectedCafeId", cafeId);
        model.addAttribute("query", q);
        return "cafe-filter-coffees/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("cafeFilterCoffeeForm", new CafeFilterCoffeeForm());
        addFormAttributes(model);
        return "cafe-filter-coffees/form";
    }

    @PostMapping("/card-extraction")
    public String extractFromCard(
            @RequestParam("image") MultipartFile image,
            Model model
    ) {
        try {
            CoffeeBeanCardExtractResult result = coffeeBeanCardExtractionService.extract(image);
            model.addAttribute("cafeFilterCoffeeForm", fromCoffeeBeanForm(result.getForm()));
            model.addAttribute("extractedRawText", result.getRawText());
            model.addAttribute("extractionWarnings", result.getWarnings());
        } catch (IllegalArgumentException exception) {
            model.addAttribute("cafeFilterCoffeeForm", new CafeFilterCoffeeForm());
            model.addAttribute("extractionWarnings", java.util.List.of(exception.getMessage()));
        }

        addFormAttributes(model);
        return "cafe-filter-coffees/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("cafeFilterCoffeeForm") CafeFilterCoffeeForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model);
            return "cafe-filter-coffees/form";
        }

        Long id = cafeFilterCoffeeService.create(form);
        redirectAttributes.addFlashAttribute("message", "카페 필터커피 기록을 등록했습니다.");
        return "redirect:/cafe-filter-coffees/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("cafeFilterCoffee", cafeFilterCoffeeService.get(id));
        return "cafe-filter-coffees/detail";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        cafeFilterCoffeeService.delete(id);
        redirectAttributes.addFlashAttribute("message", "카페 필터커피 기록을 삭제했습니다.");
        return "redirect:/cafe-filter-coffees";
    }

    private void addFormAttributes(Model model) {
        Map<FlavorCategory, java.util.List<FlavorNote>> flavorNotesByCategory = Arrays.stream(FlavorNote.values())
                .collect(Collectors.groupingBy(FlavorNote::getCategory));

        model.addAttribute("processTypes", ProcessType.values());
        model.addAttribute("countryInfos", CountryInfo.values());
        model.addAttribute("flavorCategories", FlavorCategory.values());
        model.addAttribute("flavorNotes", FlavorNote.values());
        model.addAttribute("flavorNotesByCategory", flavorNotesByCategory);
        model.addAttribute("temperatureTypes", BrewTemperatureType.values());
        model.addAttribute("brewMethods", BrewMethod.values());
        model.addAttribute("tasteScores", java.util.List.of(1, 2, 3, 4, 5));
        model.addAttribute("feelingTags", BrewFeelingTag.values());
        model.addAttribute("kakaoJavascriptKey", kakaoJavascriptKey);
        model.addAttribute("hasKakaoMapKey", StringUtils.hasText(kakaoJavascriptKey));
    }

    private CafeFilterCoffeeForm fromCoffeeBeanForm(CoffeeBeanCreateForm coffeeBeanForm) {
        CafeFilterCoffeeForm form = new CafeFilterCoffeeForm();
        form.setName(coffeeBeanForm.getName());
        form.setRoastery(coffeeBeanForm.getRoastery());
        form.setOriginCountryCode(coffeeBeanForm.getOriginCountryCode());
        form.setRegion(coffeeBeanForm.getRegion());
        form.setVariety(coffeeBeanForm.getVariety());
        form.setProcessType(coffeeBeanForm.getProcessType());
        form.setFlavorNotes(coffeeBeanForm.getFlavorNotes());
        form.setCustomFlavorNotesText(coffeeBeanForm.getCustomFlavorNotesText());
        form.setMemo(coffeeBeanForm.getMemo());
        return form;
    }
}
