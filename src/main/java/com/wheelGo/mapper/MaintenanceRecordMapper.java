package com.wheelGo.mapper;

import com.wheelGo.model.maintenance_records.MaintenanceRecord;
import com.wheelGo.model.maintenance_records.MaintenanceRecordResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceRecordMapper extends BaseMapper<MaintenanceRecordResponse,MaintenanceRecord> {
}
