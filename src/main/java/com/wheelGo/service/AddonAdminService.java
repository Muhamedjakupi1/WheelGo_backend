package com.wheelGo.service;

import com.wheelGo.mapper.AddonMapper;
import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.addon.AddonRequest;
import com.wheelGo.model.addon.AddonResponse;
import com.wheelGo.model.enums.AddonType;
import com.wheelGo.repository.AddonRepository;
import com.wheelGo.repository.BookingAddonRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AddonAdminService {
    static final String BABY_SEAT_NAME = "Baby Seat";
    static final String BLUETOOTH_NAME = "Bluetooth";

    private static final BigDecimal DEFAULT_BABY_SEAT_PRICE = new BigDecimal("25.00");
    private static final BigDecimal DEFAULT_BLUETOOTH_PRICE = new BigDecimal("10.00");

    private final AddonRepository addonRepository;
    private final BookingAddonRepository bookingAddonRepository;
    private final AddonMapper addonMapper;

    @Transactional(readOnly = true)
    public List<AddonResponse> getAll() {
        return addonMapper.toResponseList(addonRepository.findAllByIsDeletedFalseOrderByNameAsc());
    }

    @Transactional
    public List<AddonResponse> ensureInventoryAddons() {
        ensureAddon(BABY_SEAT_NAME, "Child safety seat add-on for bookings", DEFAULT_BABY_SEAT_PRICE);
        ensureAddon(BLUETOOTH_NAME, "Portable Bluetooth add-on for bookings", DEFAULT_BLUETOOTH_PRICE);
        return getAll();
    }

    @Transactional
    public AddonResponse create(AddonRequest request) {
        String name = requiredText(request.getName(), "Addon name is required");
        addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse(name)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An addon with this name already exists");
                });

        Addon addon = new Addon();
        addon.setName(name);
        addon.setDescription(trimToNull(request.getDescription()));
        addon.setPrice(normalizePrice(request.getPrice() != null ? request.getPrice() : BigDecimal.ZERO));
        addon.setQuantity(normalizeQuantity(request.getQuantity()));
        addon.setType(request.getType() != null ? request.getType() : AddonType.ONE_TIME);
        addon.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);
        addon.setInventoryManaged(true);
        addon.setIsDeleted(false);
        addon.setCreatedAt(LocalDateTime.now());
        addon.setUpdatedAt(LocalDateTime.now());
        return toResponse(addonRepository.save(addon));
    }

    @Transactional
    public AddonResponse update(UUID id, AddonRequest request) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addon not found"));

        if (request.getName() != null) {
            String name = requiredText(request.getName(), "Addon name is required");
            addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse(name)
                    .filter(existing -> !existing.getId().equals(id))
                    .ifPresent(existing -> {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "An addon with this name already exists");
                    });
            addon.setName(name);
        }
        if (request.getDescription() != null) {
            addon.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getPrice() != null) {
            addon.setPrice(normalizePrice(request.getPrice()));
        }
        if (request.getQuantity() != null) {
            if (request.getQuantity() < 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Addon quantity cannot be negative");
            }
            addon.setQuantity(request.getQuantity());
        }
        if (request.getType() != null) {
            addon.setType(request.getType());
        }
        if (request.getIsActive() != null) {
            addon.setIsActive(request.getIsActive());
        }
        addon.setUpdatedAt(LocalDateTime.now());
        return toResponse(addonRepository.save(addon));
    }

    @Transactional
    public void delete(UUID id) {
        Addon addon = addonRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Addon not found"));
        if (!bookingAddonRepository.existsByAddonId(id)) {
            addonRepository.delete(addon);
            return;
        }

        addon.setIsActive(false);
        addon.setIsDeleted(true);
        addon.setUpdatedAt(LocalDateTime.now());
        addonRepository.save(addon);
    }

    private Addon ensureAddon(String name, String description, BigDecimal price) {
        return addonRepository.findFirstByNameIgnoreCase(name)
                .map(existing -> {
                    existing.setDescription(description);
                    existing.setPrice(price);
                    existing.setType(AddonType.ONE_TIME);
                    existing.setIsActive(true);
                    existing.setInventoryManaged(true);
                    existing.setIsDeleted(false);
                    existing.setUpdatedAt(LocalDateTime.now());
                    return addonRepository.save(existing);
                })
                .orElseGet(() -> {
                    Addon addon = new Addon();
                    addon.setName(name);
                    addon.setDescription(description);
                    addon.setPrice(price);
                    addon.setQuantity(0);
                    addon.setType(AddonType.ONE_TIME);
                    addon.setIsActive(true);
                    addon.setInventoryManaged(true);
                    addon.setIsDeleted(false);
                    addon.setCreatedAt(LocalDateTime.now());
                    addon.setUpdatedAt(LocalDateTime.now());
                    return addonRepository.save(addon);
                });
    }

    private AddonResponse toResponse(Addon addon) {
        return addonMapper.toResponse(addon);
    }

    private BigDecimal normalizePrice(BigDecimal price) {
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Addon price cannot be negative");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private int normalizeQuantity(Integer quantity) {
        int normalized = quantity != null ? quantity : 0;
        if (normalized < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Addon quantity cannot be negative");
        }
        return normalized;
    }

    private String requiredText(String value, String message) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
