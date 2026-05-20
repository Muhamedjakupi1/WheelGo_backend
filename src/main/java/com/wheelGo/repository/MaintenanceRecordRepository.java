package com.wheelGo.repository;

import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, UUID> {
    List<MaintenanceRecord> findAllByOrderByPerformedAtDescCreatedAtDesc();
    List<MaintenanceRecord> findAllByVehicle_IdOrderByPerformedAtDescCreatedAtDesc(UUID vehicleId);
    boolean existsByVehicle_Id(UUID vehicleId);
    long countByVehicle_Id(UUID vehicleId);

    @Query("SELECT m FROM MaintenanceRecord m JOIN m.vehicle v WHERE " +
            "LOWER(CAST(m.type AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(m.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(m.performedBy, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.make, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.model, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(COALESCE(v.plateNumber, '')) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(m.cost AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(m.performedAt AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(CAST(m.nextDueAt AS string)) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "ORDER BY m.performedAt DESC, m.createdAt DESC")
    List<MaintenanceRecord> searchMaintenanceRecords(String keyword);
}
