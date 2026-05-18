package com.wheelGo.model.maintenance_records;


import com.wheelGo.model.enums.MaintenanceType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class MaintenanceRecordsUpdateRequest {

    @NotNull(message = "Vehicle is required")
    private UUID vehicleId;

    @NotNull(message = "Maintenance type is required")
    private MaintenanceType type;

    @Size(max = 500)
    private String description;

    @PositiveOrZero(message = "Cost cannot be negative")
    private BigDecimal cost;

    @NotNull(message = "Performance date is required")
    private LocalDateTime performedAt;

    private LocalDateTime nextDueAt;

    private String performedBy;
}
