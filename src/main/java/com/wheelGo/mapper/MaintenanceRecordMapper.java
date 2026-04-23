package com.wheelGo.mapper;

import com.wheelGo.model.maintenancerecords.MaintenanceRecord;
import com.wheelGo.model.maintenancerecords.MaintenanceRecordResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface MaintenanceRecordMapper extends BaseMapper<MaintenanceRecordResponse,MaintenanceRecord> {
}
