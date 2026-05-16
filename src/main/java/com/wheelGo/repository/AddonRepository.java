package com.wheelGo.repository;

import com.wheelGo.model.addon.Addon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AddonRepository extends JpaRepository<Addon, UUID> {
    List<Addon> findAllByIsActiveTrueAndIsDeletedFalseOrderByNameAsc();
    List<Addon> findAllByIsDeletedFalseOrderByNameAsc();
    Optional<Addon> findFirstByNameIgnoreCaseAndIsActiveTrue(String name);
    Optional<Addon> findFirstByNameIgnoreCaseAndIsDeletedFalse(String name);
    Optional<Addon> findFirstByNameIgnoreCase(String name);
}
