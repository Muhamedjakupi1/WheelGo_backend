package com.wheelGo.service;

import com.wheelGo.mapper.LocationMapper;
import com.wheelGo.model.locations.Location;
import com.wheelGo.model.locations.LocationRequest;
import com.wheelGo.model.locations.LocationResponse;
import com.wheelGo.model.locations.LocationUpdateRequest;
import com.wheelGo.repository.LocationRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LocationCrudAdminService {

    private static final String LOCATION_IN_USE_MESSAGE =
            "You cannot delete this location because vehicles are assigned to it. Reassign or delete those vehicles first.";

    private final LocationRepository locationRepository;
    private final VehicleRepository vehicleRepository;
    private final LocationMapper locationMapper;

    @Transactional(readOnly = true)
    public List<LocationResponse> getAll() {
        return locationRepository.findAll().stream()
                .sorted(Comparator.comparing(Location::getName, String.CASE_INSENSITIVE_ORDER))
                .map(locationMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LocationResponse getById(UUID id) {
        return locationMapper.toResponse(findLocation(id));
    }

    @Transactional
    public LocationResponse create(LocationRequest request) {
        Location location = new Location();
        location.setName(requiredTrimmed(request.getName(), "Location name is required"));
        location.setAddress(requiredTrimmed(request.getAddres(), "Location address is required"));
        location.setCity(requiredTrimmed(request.getCity(), "Location city is required"));
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setPhone(trimToNull(request.getPhone()));
        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public LocationResponse update(UUID id, LocationUpdateRequest request) {
        Location location = findLocation(id);

        if (request.getName() != null) {
            location.setName(requiredTrimmed(request.getName(), "Location name is required"));
        }
        if (request.getAddress() != null) {
            location.setAddress(requiredTrimmed(request.getAddress(), "Location address is required"));
        }
        if (request.getCity() != null) {
            location.setCity(requiredTrimmed(request.getCity(), "Location city is required"));
        }
        if (request.getCountry() != null) {
            location.setCountry(requiredTrimmed(request.getCountry(), "Location country is required"));
        }

        location.setLatitude(request.getLatitude() != null ? request.getLatitude() : location.getLatitude());
        location.setLongitude(request.getLongitude() != null ? request.getLongitude() : location.getLongitude());

        if (request.getPhone() != null) {
            location.setPhone(trimToNull(request.getPhone()));
        }
        if (request.getIsActive() != null) {
            location.setActive(request.getIsActive());
        }

        return locationMapper.toResponse(locationRepository.save(location));
    }

    @Transactional
    public void delete(UUID id) {
        findLocation(id);
        if (vehicleRepository.countByLocation_Id(id) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, LOCATION_IN_USE_MESSAGE);
        }

        try {
            locationRepository.deleteById(id);
            locationRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, LOCATION_IN_USE_MESSAGE, ex);
        }
    }

    private Location findLocation(UUID id) {
        return locationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Location not found"));
    }

    private String requiredTrimmed(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
