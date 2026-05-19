package com.wheelGo.repository;

import com.wheelGo.model.vehicle_categories.VehicleCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VehicleCategoryRepository extends JpaRepository<VehicleCategory, UUID> {
    List<VehicleCategory> findAllByOrderByNameAsc();
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, UUID id);

    @Query("SELECT c FROM VehicleCategory c WHERE " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY c.name ASC")
    List<VehicleCategory> searchVehicleCategories(String keyword);
}
