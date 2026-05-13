package com.wheelGo.service;

import com.wheelGo.config.FileStorageProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    private final Path uploadRoot;

    public FileStorageService(FileStorageProperties fileStorageProperties) {
        this.uploadRoot = Paths.get(fileStorageProperties.uploadDir()).toAbsolutePath().normalize();
    }

    public String storeVehicleImage(MultipartFile file) {
        return storeImage(file, "vehicle-images", null);
    }

    public String storeDriverLicenseImage(MultipartFile file, String side) {
        return storeImage(file, "driver-license-images", side);
    }

    public Path resolveStoredUpload(String relativeUrl) {
        if (relativeUrl == null || relativeUrl.isBlank() || !relativeUrl.startsWith("/uploads/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stored upload path is invalid");
        }

        String normalized = relativeUrl.substring("/uploads/".length()).replace("/", java.io.File.separator);
        Path resolved = uploadRoot.resolve(normalized).normalize();
        if (!resolved.startsWith(uploadRoot) || !Files.exists(resolved)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored upload file not found");
        }
        return resolved;
    }

    private String storeImage(MultipartFile file, String folder, String filePrefix) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "" : file.getOriginalFilename());
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only image files are allowed");
        }

        try {
            Path targetDir = uploadRoot.resolve(folder);
            Files.createDirectories(targetDir);

            String fileName = (filePrefix == null || filePrefix.isBlank() ? "" : filePrefix + "-") + UUID.randomUUID() + "." + extension;
            Path targetFile = targetDir.resolve(fileName).normalize();

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }

            return "/uploads/" + folder + "/" + fileName;
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store image file");
        }
    }

    private String extractExtension(String filename) {
        int index = filename.lastIndexOf('.');
        if (index < 0 || index == filename.length() - 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file extension is missing");
        }
        return filename.substring(index + 1).toLowerCase();
    }
}
