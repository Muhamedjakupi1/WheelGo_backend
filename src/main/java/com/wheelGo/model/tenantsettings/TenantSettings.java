package com.wheelGo.model.tenantsettings;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tenant_settings")
@Getter @Setter @NoArgsConstructor
public class TenantSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(nullable = false)
    private String timezone = "UTC";

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "theme_color")
    private String themeColor = "#1A73E8";

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();
}