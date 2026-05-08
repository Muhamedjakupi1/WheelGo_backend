package com.wheelGo.repository;

import com.wheelGo.model.vehicle_images.VehicleImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleImageRepository extends JpaRepository<VehicleImage, UUID> {
    List<VehicleImage> findAllByOrderByUploadedAtDesc();
    List<VehicleImage> findByVehicleIdOrderByUploadedAtDesc(UUID vehicleId);
    List<VehicleImage> findByVehicleIdInOrderByUploadedAtDesc(List<UUID> vehicleIds);
}
