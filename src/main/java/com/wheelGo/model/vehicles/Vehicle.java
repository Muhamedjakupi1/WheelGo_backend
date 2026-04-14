package com.wheelGo.model.vehicles;

import com.wheelGo.model.locations.Location;
import com.wheelGo.model.vehicleCategories.VehicleCategory;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vehicles")
@Getter @Setter

public class Vehicle {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private VehicleCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private Location location;

    @Column(name = "plate_number", nullable = false, unique = true, length = 20)
    private String plateNumber;

    @Column(nullable = false, length = 60)
    private String make;

    @Column(nullable = false, length = 60)
    private String model;

    @Column(nullable = false)
    private Short year;

    @Column(length = 40)
    private String color;

    @Column(unique = true, length = 20)
    private String vin;

    @Column(name = "fuel_type", nullable = false)
    private String fuelType = "PETROL";

    @Column(nullable = false)
    private String transmission = "MANUAL";

    @Column(nullable = false)
    private Short seats = 5;

    @Column(name = "daily_rate", nullable = false, precision = 10, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false)
    private String status = "AVAILABLE";

    @Column(nullable = false)
    private Integer mileage = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
