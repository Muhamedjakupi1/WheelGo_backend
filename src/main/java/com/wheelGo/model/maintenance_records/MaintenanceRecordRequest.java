package com.wheelGo.model.maintenance_records;

import com.wheelGo.model.enums.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class MaintenanceRecordRequest {
    @NotNull(message = "ID of car is required")
    private UUID vehicleId;

    @NotNull(message = "Type of service is required")
    private MaintenanceType type;

    private String description;

    @PositiveOrZero
    private BigDecimal cost;

    @NotNull(message = "Performed date is required")
    private LocalDateTime performedAt;

    private LocalDateTime nextDueAt;

    private String performedBy;
}
