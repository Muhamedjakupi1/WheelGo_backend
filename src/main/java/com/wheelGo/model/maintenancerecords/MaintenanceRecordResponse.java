package com.wheelGo.model.maintenanceRecords;

import com.wheelGo.model.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class MaintenanceRecordResponse {
    private UUID id;
    private UUID vehicleId;
    private MaintenanceType type;
    private String description;
    private BigDecimal cost;
    private LocalDateTime performedAt;
    private LocalDateTime nextDueAt;
    private String performedBy;
}
