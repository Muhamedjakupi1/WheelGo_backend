package com.wheelGo.repository;

import com.wheelGo.model.vehicles.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {
    List<Vehicle> findAllByOrderByCreatedAtDesc();

    boolean existsByCategory_Id(UUID categoryId);
    long countByCategory_Id(UUID categoryId);

    boolean existsByPlateNumberIgnoreCase(String plateNumber);
    boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, UUID id);
    boolean existsByVinIgnoreCase(String vin);
    boolean existsByVinIgnoreCaseAndIdNot(String vin, UUID id);
}
