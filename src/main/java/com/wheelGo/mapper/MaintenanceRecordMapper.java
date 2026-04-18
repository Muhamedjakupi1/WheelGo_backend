package com.wheelGo.mapper;

import com.wheelGo.model.maintenanceRecords.MaintenanceRecord;
import com.wheelGo.model.maintenanceRecords.MaintenanceRecordResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceRecordMapper extends BaseMapper<MaintenanceRecordResponse,MaintenanceRecord> {
}
