package com.wheelGo.model.reviews;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table (name = "reviews")
@Getter @Setter @NoArgsConstructor
public class Review {
    @Id
    @GeneratedValue (strategy = GenerationType.UUID)
    private UUID id;

    @Column (name = "booking_id",nullable = false, unique = true)
    private UUID bookingId;

    @Column (name = "user_id", nullable = false)
    private UUID userId;

    @Column (name = "vehicle_id", nullable = false)
    private UUID vehicleId;

    @Column (name = "rating", nullable = false)
    private int rating;

    @Column (name = "comment")
    private String comment;

    @Column (name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

}
