package com.wheelGo.controller;

import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.addon.AddonResponse;
import com.wheelGo.repository.AddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN', 'SUPER_ADMIN')")
public class AddonController {
    private final AddonRepository addonRepository;

    @GetMapping
    public ResponseEntity<List<AddonResponse>> getActiveAddons() {
        return ResponseEntity.ok(addonRepository.findAllByIsActiveTrueOrderByNameAsc().stream()
                .map(this::toResponse)
                .toList());
    }

    private AddonResponse toResponse(Addon addon) {
        AddonResponse response = new AddonResponse();
        response.setId(addon.getId());
        response.setName(addon.getName());
        response.setDescription(addon.getDescription());
        response.setPrice(addon.getPrice());
        response.setQuantity(addon.getQuantity());
        response.setType(addon.getType());
        response.setIsActive(addon.getIsActive());
        response.setCreatedAt(addon.getCreatedAt());
        response.setUpdatedAt(addon.getUpdatedAt());
        return response;
    }
}
