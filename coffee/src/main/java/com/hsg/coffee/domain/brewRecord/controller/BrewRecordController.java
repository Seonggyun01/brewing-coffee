package com.hsg.coffee.domain.brewRecord.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.hsg.coffee.domain.brewRecord.dto.BrewRecordForm;
import com.hsg.coffee.domain.brewRecord.entity.BrewFeelingTag;
import com.hsg.coffee.domain.brewRecord.entity.BrewMethod;
import com.hsg.coffee.domain.brewRecord.entity.FlavorCategory;
import com.hsg.coffee.domain.brewRecord.entity.FlavorNote;
import com.hsg.coffee.domain.brewRecord.service.BrewRecordService;
import com.hsg.coffee.domain.coffeeBean.service.CoffeeBeanService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/brew-records")
public class BrewRecordController {

    private final BrewRecordService brewRecordService;
    private final CoffeeBeanService coffeeBeanService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("brewRecords", brewRecordService.getAll());
        return "brew-records/list";
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("brewRecordForm", new BrewRecordForm());
        addFormAttributes(model, "브루잉 기록 등록", "/brew-records");
        return "brew-records/form";
    }

    @PostMapping
    public String create(
            @Valid @ModelAttribute("brewRecordForm") BrewRecordForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "브루잉 기록 등록", "/brew-records");
            return "brew-records/form";
        }

        Long id = brewRecordService.create(form);
        redirectAttributes.addFlashAttribute("message", "브루잉 기록을 등록했습니다.");
        return "redirect:/brew-records/" + id;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("brewRecord", brewRecordService.get(id));
        return "brew-records/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("brewRecordForm", brewRecordService.getUpdateForm(id));
        addFormAttributes(model, "브루잉 기록 수정", "/brew-records/" + id + "/edit");
        return "brew-records/form";
    }

    @PostMapping("/{id}/edit")
    public String update(
            @PathVariable Long id,
            @Valid @ModelAttribute("brewRecordForm") BrewRecordForm form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            addFormAttributes(model, "브루잉 기록 수정", "/brew-records/" + id + "/edit");
            return "brew-records/form";
        }

        brewRecordService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "브루잉 기록을 수정했습니다.");
        return "redirect:/brew-records/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        brewRecordService.delete(id);
        redirectAttributes.addFlashAttribute("message", "브루잉 기록을 삭제했습니다.");
        return "redirect:/brew-records";
    }

    private void addFormAttributes(Model model, String title, String actionUrl) {
        Map<FlavorCategory, java.util.List<FlavorNote>> flavorNotesByCategory = Arrays.stream(FlavorNote.values())
                .collect(Collectors.groupingBy(FlavorNote::getCategory));

        model.addAttribute("title", title);
        model.addAttribute("actionUrl", actionUrl);
        model.addAttribute("coffeeBeans", coffeeBeanService.getAll());
        model.addAttribute("brewMethods", BrewMethod.values());
        model.addAttribute("tasteScores", java.util.List.of(1, 2, 3, 4, 5));
        model.addAttribute("flavorCategories", FlavorCategory.values());
        model.addAttribute("flavorNotesByCategory", flavorNotesByCategory);
        model.addAttribute("feelingTags", BrewFeelingTag.values());
    }
}
