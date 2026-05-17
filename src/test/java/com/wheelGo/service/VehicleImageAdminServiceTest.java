package com.wheelGo.service;

import com.wheelGo.model.vehicle_images.VehicleImage;
import com.wheelGo.model.vehicle_images.VehicleImageResponse;
import com.wheelGo.model.vehicles.Vehicle;
import com.wheelGo.repository.VehicleImageRepository;
import com.wheelGo.repository.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleImageAdminServiceTest {

    @Mock private VehicleImageRepository vehicleImageRepository;
    @Mock private VehicleRepository vehicleRepository;
    @Mock private FileStorageService fileStorageService;
    @InjectMocks private VehicleImageAdminService vehicleImageAdminService;

    private UUID vehicleId;
    private UUID imageId;
    private Vehicle vehicle;
    private VehicleImage image;

    @BeforeEach
    void setUp() {
        vehicleId = UUID.randomUUID();
        imageId = UUID.randomUUID();
        vehicle = new Vehicle();
        vehicle.setId(vehicleId);
        vehicle.setMake("BMW");
        vehicle.setModel("X5");
        vehicle.setPlateNumber("01-123-AA");

        image = new VehicleImage();
        image.setId(imageId);
        image.setVehicle(vehicle);
        image.setUrl("/uploads/car.png");
        image.setPrimary(true);
    }

    @Test
    void should_return_all_images_when_get_all_without_vehicle_id() {
        when(vehicleImageRepository.findAllByOrderByUploadedAtDesc()).thenReturn(List.of(image));

        List<VehicleImageResponse> result = vehicleImageAdminService.getAll(null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getVehicleLabel()).contains("BMW X5");
    }

    @Test
    void should_return_image_when_get_by_id_found() {
        when(vehicleImageRepository.findById(imageId)).thenReturn(Optional.of(image));

        VehicleImageResponse result = vehicleImageAdminService.getById(imageId);

        assertThat(result.getUrl()).isEqualTo("/uploads/car.png");
    }

    @Test
    void should_throw_bad_request_when_creating_upload_without_file() {
        MockMultipartFile empty = new MockMultipartFile("file", new byte[0]);

        assertThatThrownBy(() -> vehicleImageAdminService.createFromUpload(vehicleId, empty, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An image file is required");
    }

    @Test
    void should_create_image_when_upload_valid() {
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1});
        when(fileStorageService.storeVehicleImage(file)).thenReturn("/uploads/car.png");
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)).thenReturn(List.of());
        when(vehicleImageRepository.save(any(VehicleImage.class))).thenAnswer(invocation -> {
            VehicleImage saved = invocation.getArgument(0);
            saved.setId(imageId);
            return saved;
        });

        VehicleImageResponse result = vehicleImageAdminService.createFromUpload(vehicleId, file, true);

        assertThat(result.getVehicleId()).isEqualTo(vehicleId);
        assertThat(result.isPrimary()).isTrue();
    }

    @Test
    void should_throw_not_found_when_updating_missing_image() {
        when(vehicleImageRepository.findById(imageId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleImageAdminService.update(imageId, null, true))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Vehicle image not found");
    }

    @Test
    void should_throw_bad_request_when_update_has_no_changes() {
        when(vehicleImageRepository.findById(imageId)).thenReturn(Optional.of(image));

        assertThatThrownBy(() -> vehicleImageAdminService.update(imageId, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("replacement file and/or primary flag changes");
    }

    @Test
    void should_update_image_when_file_and_primary_provided() {
        MockMultipartFile file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1});
        VehicleImage other = new VehicleImage();
        other.setVehicle(vehicle);
        other.setPrimary(true);

        when(vehicleImageRepository.findById(imageId)).thenReturn(Optional.of(image));
        when(fileStorageService.storeVehicleImage(file)).thenReturn("/uploads/new.png");
        when(vehicleImageRepository.findByVehicleIdOrderByUploadedAtDesc(vehicleId)).thenReturn(List.of(other, image));
        when(vehicleImageRepository.save(image)).thenReturn(image);

        VehicleImageResponse result = vehicleImageAdminService.update(imageId, file, true);

        assertThat(image.getUrl()).isEqualTo("/uploads/new.png");
        assertThat(other.isPrimary()).isFalse();
        assertThat(result.getUrl()).isEqualTo("/uploads/new.png");
    }

    @Test
    void should_delete_image_when_found() {
        when(vehicleImageRepository.findById(imageId)).thenReturn(Optional.of(image));

        vehicleImageAdminService.delete(imageId);

        verify(vehicleImageRepository).delete(image);
    }
}
