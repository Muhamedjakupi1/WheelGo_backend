package com.wheelGo.controller;

import com.wheelGo.model.promotions.PromotionRequest;
import com.wheelGo.model.promotions.PromotionResponse;
import com.wheelGo.service.PromotionAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/promotions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class PromotionAdminController {
    private final PromotionAdminService promotionAdminService;

    @GetMapping
    public ResponseEntity<List<PromotionResponse>> getAll() {
        return ResponseEntity.ok(promotionAdminService.getAll());
    }

    @PostMapping
    public ResponseEntity<PromotionResponse> create(@RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionAdminService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<PromotionResponse> update(@PathVariable UUID id, @RequestBody PromotionRequest request) {
        return ResponseEntity.ok(promotionAdminService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promotionAdminService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
