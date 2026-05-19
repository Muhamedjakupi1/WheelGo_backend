package com.wheelGo.repository;

import com.wheelGo.model.bookings.BookingResponse;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findAllByOrderByCreatedAtDesc();

    boolean existsByCategory_Id(UUID categoryId);
    long countByCategory_Id(UUID categoryId);
    long countByLocation_Id(UUID locationId);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, UUID id);
    boolean existsByVinIgnoreCase(String vin);
    boolean existsByVinIgnoreCaseAndIdNot(String vin, UUID id);

    @Query("SELECT v from Vehicle v " +
            "LEFT JOIN v.category c " +
            "LEFT JOIN v.location l " +
            "WHERE LOWER(v.make) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(c.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(l.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(v.fuelType AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(v.transmission AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(v.status AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(v.year AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.color, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY v.createdAt DESC")
    List<Vehicle> searchVehicle(String keyword);
}
