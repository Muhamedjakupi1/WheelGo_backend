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

    public static MaintenanceRecordResponse from(MaintenanceRecord record) {
        MaintenanceRecordResponse res = new MaintenanceRecordResponse();
        res.setId(record.getId());
        res.setVehicleId(record.getVehicle().getId());
        res.setType(record.getType());
        res.setDescription(record.getDescription());
        res.setCost(record.getCost());
        res.setPerformedAt(record.getPerformedAt());
        res.setNextDueAt(record.getNextDueAt());
        res.setPerformedBy(record.getPerformedBy());
        return res;
    }
}
