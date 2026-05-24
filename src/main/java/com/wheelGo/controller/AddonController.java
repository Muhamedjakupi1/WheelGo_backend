package com.wheelGo.controller;

import com.wheelGo.mapper.AddonMapper;
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
    private final AddonMapper addonMapper;

    @GetMapping
    public ResponseEntity<List<AddonResponse>> getActiveAddons() {
        return ResponseEntity.ok(addonMapper.toResponseList(
                addonRepository.findAllByIsActiveTrueAndIsDeletedFalseOrderByNameAsc()
        ));
    }
}
