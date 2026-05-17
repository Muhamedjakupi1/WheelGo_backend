package com.wheelGo.model.maintenance_records;

import com.wheelGo.model.enums.MaintenanceType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class MaintenanceRecordResponse {
    private UUID id;
    private UUID vehicleId;
    private String vehicleName;
    private String plateNumber;
    private MaintenanceType type;
    private String description;
    private BigDecimal cost;
    private LocalDateTime performedAt;
    private LocalDateTime nextDueAt;
    private String performedBy;
    private LocalDateTime createdAt;
}
