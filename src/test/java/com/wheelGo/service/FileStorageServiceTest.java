package com.wheelGo.service;

import com.wheelGo.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private FileStorageProperties fileStorageProperties;

    private FileStorageService fileStorageService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        when(fileStorageProperties.uploadDir()).thenReturn(tempDir.toString());
        fileStorageService = new FileStorageService(fileStorageProperties);
    }

    @Test
    void should_store_vehicle_image_when_file_valid() {
        MockMultipartFile file = new MockMultipartFile("file", "car.png", "image/png", new byte[]{1, 2});

        String result = fileStorageService.storeVehicleImage(file);

        assertThat(result).startsWith("/uploads/vehicle-images/");
    }

    @Test
    void should_throw_bad_request_when_image_extension_missing() {
        MockMultipartFile file = new MockMultipartFile("file", "car", "image/png", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.storeVehicleImage(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("extension is missing");
    }

    @Test
    void should_throw_bad_request_when_extension_not_allowed() {
        MockMultipartFile file = new MockMultipartFile("file", "car.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> fileStorageService.storeVehicleImage(file))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Only image files are allowed");
    }

    @Test
    void should_resolve_stored_upload_when_file_exists() throws Exception {
        Path folder = Files.createDirectories(tempDir.resolve("vehicle-images"));
        Path file = Files.writeString(folder.resolve("test.png"), "x");

        Path result = fileStorageService.resolveStoredUpload("/uploads/vehicle-images/test.png");

        assertThat(result).isEqualTo(file);
    }

    @Test
    void should_throw_not_found_when_resolved_upload_missing() {
        assertThatThrownBy(() -> fileStorageService.resolveStoredUpload("/uploads/vehicle-images/missing.png"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Stored upload file not found");
    }
}
