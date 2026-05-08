package com.wheelGo.repository;

import com.wheelGo.model.vehicle_categories.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, UUID> {
    List<VehicleCategory> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);
}
