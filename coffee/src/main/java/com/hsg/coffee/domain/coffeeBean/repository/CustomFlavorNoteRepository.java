package com.hsg.coffee.domain.coffeeBean.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hsg.coffee.domain.coffeeBean.entity.CustomFlavorNote;

public interface CustomFlavorNoteRepository extends JpaRepository<CustomFlavorNote, Long> {

    Optional<CustomFlavorNote> findByNormalizedName(String normalizedName);

    List<CustomFlavorNote> findByNormalizedNameIn(Collection<String> normalizedNames);
}
