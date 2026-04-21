package com.wheelGo.model.enums;

@PgEnumType(value = "maintenance_type", scope = PgEnumScope.TENANT)
public enum MaintenanceType {
    OIL_CHANGE,
    TIRE,
    INSPECTION,
    REPAIR
}
