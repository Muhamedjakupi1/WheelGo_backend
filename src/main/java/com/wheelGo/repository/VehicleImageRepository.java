package com.wheelGo.repository;

import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicles.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, UUID> {
    List<VehicleImage> findAllByOrderByUploadedAtDesc();
    List<VehicleImage> findByVehicleIdOrderByUploadedAtDesc(UUID vehicleId);
    List<VehicleImage> findByVehicleIdInOrderByUploadedAtDesc(List<UUID> vehicleIds);

    @Query("SELECT i FROM VehicleImage i JOIN i.vehicle v WHERE " +
            "(:vehicleId IS NULL OR v.id = :vehicleId) AND (" +
            "LOWER(i.url) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.make) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.model) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(v.plateNumber) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "ORDER BY i.uploadedAt DESC")
    List<VehicleImage> searchVehicleImages(UUID vehicleId, String keyword);
}
