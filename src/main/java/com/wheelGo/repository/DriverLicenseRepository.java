package com.wheelGo.repository;

import com.wheelGo.model.driver_licenses.DriverLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DriverLicenseRepository extends JpaRepository<DriverLicense, UUID> {
    Optional<DriverLicense> findByUser_Id(UUID userId);
}
