package com.wheelGo.service;

import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.model.vehicles.VehicleResponse;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleImageAdminService {

    private final VehicleImageRepository vehicleImageRepository;
    private final VehicleRepository vehicleRepository;
    private final FileStorageService fileStorageService;
    private final CacheInvalidationService cacheInvalidationService;

    @Transactional(readOnly = true)
    public List<VehicleImageResponse> getAll(UUID vehicleId) {
        return getAll(vehicleId, null);
    }

    @Transactional(readOnly = true)
    public List<VehicleImageResponse> getAll(UUID vehicleId, String keyword) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        List<VehicleImage> images = normalizedKeyword != null && !normalizedKeyword.isEmpty()
                ? vehicleImageRepository.searchVehicleImages(vehicleId, normalizedKeyword)
                : vehicleId != null
                ? vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)
                : vehicleImageRepository.findAllByOrderByUploadedAtDesc();
        return images.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleImageResponse getById(UUID id) {
        return toResponse(findImage(id));
    }

    @Transactional
    public VehicleImageResponse createFromUpload(UUID vehicleId, MultipartFile file, boolean isPrimary) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An image file is required");
        }
        String storedUrl = fileStorageService.storeVehicleImage(file);
        Vehicle vehicle = findVehicle(vehicleId);
        VehicleImageResponse response = toResponse(saveNewVehicleImageRow(vehicle, storedUrl, isPrimary));
        cacheInvalidationService.evictVehicle(vehicle.getId());
        return response;
    }

    @Transactional
    public VehicleImageResponse update(UUID id, MultipartFile file, Boolean isPrimary) {
        VehicleImage image = findImage(id);
        boolean hasFile = file != null && !file.isEmpty();

        if (isPrimary == null && !hasFile) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide a replacement file and/or primary flag changes");
        }

        if (hasFile) {
            String storedUrl = fileStorageService.storeVehicleImage(file);
            image.setUrl(storedUrl);
        }

        if (isPrimary != null) {
            if (Boolean.TRUE.equals(isPrimary)) {
                clearPrimaryFlag(image.getVehicle().getId());
                image.setPrimary(true);
            } else {
                image.setPrimary(false);
            }
        }

        VehicleImageResponse response = toResponse(vehicleImageRepository.save(image));
        cacheInvalidationService.evictVehicle(image.getVehicle().getId());
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        VehicleImage image = findImage(id);
        UUID vehicleId = image.getVehicle() != null ? image.getVehicle().getId() : null;
        vehicleImageRepository.delete(image);
        cacheInvalidationService.evictVehicle(vehicleId);
    }

    private VehicleImage saveNewVehicleImageRow(Vehicle vehicle, String url, boolean isPrimary) {
        if (isPrimary) {
            clearPrimaryFlag(vehicle.getId());
        }
        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setUrl(url.trim());
        image.setPrimary(isPrimary);
        return vehicleImageRepository.save(image);
    }

    private void clearPrimaryFlag(UUID vehicleId) {
        vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)
                .forEach(existing -> existing.setPrimary(false));
    }

    private VehicleImage findImage(UUID id) {
        return vehicleImageRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle image not found"));
    }

    private Vehicle findVehicle(UUID id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vehicle not found"));
    }

    private VehicleImageResponse toResponse(VehicleImage image) {
        VehicleImageResponse response = new VehicleImageResponse();
        response.setId(image.getId());
        response.setVehicleId(image.getVehicle() != null ? image.getVehicle().getId() : null);
        response.setVehicleLabel(image.getVehicle() != null
                ? image.getVehicle().getMake() + " " + image.getVehicle().getModel() + " (" + image.getVehicle().getPlateNumber() + ")"
                : null);
        response.setUrl(image.getUrl());
        response.setPrimary(image.isPrimary());
        return response;
    }
}
