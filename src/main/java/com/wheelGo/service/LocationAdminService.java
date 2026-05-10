package com.wheelGo.service;

import com.wheelGo.model.locations.LocationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationAdminService {

    private final LocationCrudAdminService locationCrudAdminService;

    @Transactional(readOnly = true)
    public List<LocationResponse> getAll() {
        return locationCrudAdminService.getAll();
    }
}
