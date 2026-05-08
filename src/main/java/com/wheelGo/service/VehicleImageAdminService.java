package com.wheelGo.service;

import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicle_images.VehicleImageRequest;
import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import com.wheelGo.model.vehicle_images.VehicleImagesUpdateRequest;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional(readOnly = true)
    public List<VehicleImageResponse> getAll(UUID vehicleId) {
        List<VehicleImage> images = vehicleId != null
                ? vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)
                : vehicleImageRepository.findAllByOrderByUploadedAtDesc();
        return images.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public VehicleImageResponse getById(UUID id) {
        return toResponse(findImage(id));
    }

    @Transactional
    public VehicleImageResponse create(VehicleImageRequest request) {
        Vehicle vehicle = findVehicle(request.getVehicleId());
        if (request.isPrimary()) {
            clearPrimaryFlag(vehicle.getId());
        }

        VehicleImage image = new VehicleImage();
        image.setVehicle(vehicle);
        image.setUrl(request.getUrl().trim());
        image.setPrimary(request.isPrimary());
        return toResponse(vehicleImageRepository.save(image));
    }

    @Transactional
    public VehicleImageResponse createFromUpload(UUID vehicleId, MultipartFile file, boolean isPrimary) {
        String storedUrl = fileStorageService.storeVehicleImage(file);

        VehicleImageRequest request = new VehicleImageRequest();
        request.setVehicleId(vehicleId);
        request.setUrl(storedUrl);
        request.setPrimary(isPrimary);

        return create(request);
    }

    @Transactional
    public VehicleImageResponse update(UUID id, VehicleImagesUpdateRequest request) {
        VehicleImage image = findImage(id);

        if (request.getIsPrimary() != null && request.getIsPrimary()) {
            clearPrimaryFlag(image.getVehicle().getId());
        }

        if (request.getUrl() != null && !request.getUrl().isBlank()) {
            image.setUrl(request.getUrl().trim());
        }

        if (request.getIsPrimary() != null) {
            image.setPrimary(request.getIsPrimary());
        }

        return toResponse(vehicleImageRepository.save(image));
    }

    @Transactional
    public void delete(UUID id) {
        vehicleImageRepository.delete(findImage(id));
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
