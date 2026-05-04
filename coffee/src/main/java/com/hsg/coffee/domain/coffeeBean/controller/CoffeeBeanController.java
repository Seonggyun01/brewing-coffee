package com.hsg.coffee.domain.coffeeBean.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import com.hsg.coffee.domain.brewRecord.entity.FlavorCategory;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanCreateForm;
import com.hsg.coffee.domain.coffeeBean.dto.CoffeeBeanUpdateForm;
import com.hsg.coffee.domain.coffeeBean.entity.CoffeeBeanStatus;
import com.hsg.coffee.domain.coffeeBean.entity.ProcessType;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;
import com.hsg.coffee.domain.purchasePlace.entity.PurchasePlaceType;
import com.hsg.coffee.domain.purchasePlace.service.PurchasePlaceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/coffee-beans")
public class CoffeeBeanController {

    private final CoffeeBeanService coffeeBeanService;
    private final PurchasePlaceService purchasePlaceService;

    @GetMapping
    public String list(@RequestParam(required = false) String keyword, Model model) {
        model.addAttribute("coffeeBeans", coffeeBeanService.searchByName(keyword));
        model.addAttribute("keyword", keyword);
        return "coffee-beans/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("coffeeBeanForm", new CoffeeBeanCreateForm());
        addFormAttributes(model, "원두 등록", "/coffee-beans");
        return "coffee-beans/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("coffeeBeanForm") CoffeeBeanCreateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "원두 등록", "/coffee-beans");
            return "coffee-beans/form";
        }

        Long id = coffeeBeanService.create(form);
        redirectAttributes.addFlashAttribute("message", "원두를 등록했습니다.");
        return "redirect:/coffee-beans/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("coffeeBean", coffeeBeanService.get(id));
        return "coffee-beans/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("coffeeBeanForm", coffeeBeanService.getUpdateForm(id));
        addFormAttributes(model, "원두 수정", "/coffee-beans/" + id + "/edit");
        return "coffee-beans/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("coffeeBeanForm") CoffeeBeanUpdateForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "원두 수정", "/coffee-beans/" + id + "/edit");
            return "coffee-beans/form";
        }

        coffeeBeanService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "원두를 수정했습니다.");
        return "redirect:/coffee-beans/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        coffeeBeanService.delete(id);
        redirectAttributes.addFlashAttribute("message", "원두를 삭제했습니다.");
        return "redirect:/coffee-beans";
    }

    private void addFormAttributes(Model model, String title, String actionUrl) {
        Map<FlavorCategory, java.util.List<FlavorNote>> flavorNotesByCategory = Arrays.stream(FlavorNote.values())
                .collect(Collectors.groupingBy(FlavorNote::getCategory));

        model.addAttribute("title", title);
        model.addAttribute("actionUrl", actionUrl);
        model.addAttribute("processTypes", ProcessType.values());
        model.addAttribute("coffeeBeanStatuses", CoffeeBeanStatus.values());
        model.addAttribute("purchasePlaceTypes", PurchasePlaceType.values());
        model.addAttribute("purchasePlaces", purchasePlaceService.getAll());
        model.addAttribute("flavorCategories", FlavorCategory.values());
        model.addAttribute("flavorNotesByCategory", flavorNotesByCategory);
    }
}
