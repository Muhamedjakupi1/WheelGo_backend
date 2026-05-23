package com.wheelGo.repository;

import com.wheelGo.model.promotions.Promotion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PromotionRepository extends JpaRepository<Promotion, UUID> {
    List<Promotion> findAllByOrderByCreatedAtDesc();

    Optional<Promotion> findFirstByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);
}
