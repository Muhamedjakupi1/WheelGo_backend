package com.wheelGo.model.maintenance_records;


import com.wheelGo.model.enums.MaintenanceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class MaintenanceRecordsUpdateRequest {

    @NotBlank(message = "Maintenance type is required")
    private MaintenanceType type;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Cost is required")
    private BigDecimal cost;

    @NotNull(message = "Performance date is required")
    private LocalDateTime performedAt;

    private LocalDateTime nextDueAt;

    private String performedBy;;
}
