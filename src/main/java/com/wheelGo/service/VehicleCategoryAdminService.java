package com.wheelGo.service;

import com.wheelGo.model.vehicle_categories.VehicleCategory;
import com.wheelGo.model.vehicle_categories.VehicleCategoryRequest;
import com.wheelGo.model.vehicle_categories.VehicleCategoryResponse;
import com.wheelGo.model.vehicle_categories.VehicleCategoryUpdateRequest;
import com.wheelGo.repository.VehicleCategoryRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleCategoryAdminService {

    private final VehicleCategoryRepository vehicleCategoryRepository;
    private final VehicleRepository vehicleRepository;

    @Transactional(readOnly = true)
    public List<VehicleCategoryResponse> getAll() {
        return vehicleCategoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleCategoryResponse getById(UUID id) {
        return toResponse(findCategory(id));
    }

    @Transactional
    public VehicleCategoryResponse create(VehicleCategoryRequest request) {
        String normalizedName = normalize(request.getName());
        if (vehicleCategoryRepository.existsByNameIgnoreCase(normalizedName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle category name already exists");
        }

        VehicleCategory category = new VehicleCategory();
        category.setName(normalizedName);
        category.setDescription(trimToNull(request.getDescription()));
        return toResponse(vehicleCategoryRepository.save(category));
    }

    @Transactional
    public VehicleCategoryResponse update(UUID id, VehicleCategoryUpdateRequest request) {
        VehicleCategory category = findCategory(id);

        if (request.getName() != null && !request.getName().isBlank()) {
            String normalizedName = normalize(request.getName());
            if (vehicleCategoryRepository.existsByNameIgnoreCaseAndIdNot(normalizedName, id)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle category name already exists");
            }
            category.setName(normalizedName);
        }

        if (request.getDescription() != null) {
            category.setDescription(trimToNull(request.getDescription()));
        }

        return toResponse(vehicleCategoryRepository.save(category));
    }

    @Transactional
    public void delete(UUID id) {
        findCategory(id);
        if (vehicleRepository.existsByCategory_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This category cannot be deleted while vehicles are assigned to it. Reassign or remove those vehicles first."
            );
        }
        vehicleCategoryRepository.deleteById(id);
    }

    private VehicleCategory findCategory(UUID id) {
        return vehicleCategoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle category not found"));
    }

    private VehicleCategoryResponse toResponse(VehicleCategory category) {
        VehicleCategoryResponse response = new VehicleCategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        return response;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
