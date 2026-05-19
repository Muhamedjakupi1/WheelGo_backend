package com.wheelGo.repository;

import com.wheelGo.model.locations.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface LocationRepository extends JpaRepository<Location, UUID> {
    @Query("SELECT l FROM Location l WHERE " +
            "LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.address) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.city) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.country) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(l.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY l.name ASC")
    List<Location> searchLocations(String keyword);
}
