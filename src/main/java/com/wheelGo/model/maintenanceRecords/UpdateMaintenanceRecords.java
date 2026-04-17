package com.wheelGo.model.maintenanceRecords;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateMaintenanceRecords {

    @NotBlank(message = "Maintenance type is required")
    private String type;

    @Size(max = 500)
    private String description;

    @NotNull(message = "Cost is required")
    private BigDecimal cost;

    @NotNull(message = "Performance date is required")
    private LocalDateTime performedAt;

    private LocalDateTime nextDueAt;

    private String performedBy;;
}
