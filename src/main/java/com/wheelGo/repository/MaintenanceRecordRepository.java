package com.wheelGo.repository;

import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findAllByOrderByPerformedAtDescCreatedAtDesc();
    List<MaintenanceRecord> findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(UUID vehicleId);
    boolean existsByVehicle_Id(UUID vehicleId);
    long countByVehicle_Id(UUID vehicleId);
}
