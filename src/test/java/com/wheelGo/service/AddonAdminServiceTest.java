package com.wheelGo.service;

import com.wheelGo.mapper.AddonMapper;
import com.wheelGo.model.addon.Addon;
import com.wheelGo.model.addon.AddonRequest;
import com.wheelGo.model.addon.AddonResponse;
import com.wheelGo.model.enums.AddonType;
import com.wheelGo.repository.AddonRepository;
import com.wheelGo.repository.BookingAddonRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AddonAdminServiceTest {


    @Mock
    private AddonRepository addonRepository;

    @Mock
    private BookingAddonRepository bookingAddonRepository;

    @Mock
    private AddonMapper addonMapper;

    @InjectMocks
    private AddonAdminService addonAdminService;

    @Captor
    private ArgumentCaptor<Addon> addonCaptor;

    private Addon addon;

    @BeforeEach
    void setUp() {
        addon = new Addon();
        addon.setId(UUID.randomUUID());
        addon.setName("Bluetooth");
        addon.setDescription("Portable device");
        addon.setPrice(new BigDecimal("10.00"));
        addon.setQuantity(3);
        addon.setType(AddonType.ONE_TIME);
        addon.setIsActive(true);
        addon.setInventoryManaged(true);
        addon.setIsDeleted(false);

        lenient().when(addonMapper.toResponse(any(Addon.class))).thenAnswer(invocation -> toResponse(invocation.getArgument(0)));
        lenient().when(addonMapper.toResponseList(anyList())).thenAnswer(invocation -> invocation.<List<Addon>>getArgument(0).stream()
                .map(this::toResponse)
                .toList());
    }

    @Test
    void should_return_all_addons_when_get_all() {
        when(addonRepository.findAllByIsDeletedFalseOrderByNameAsc()).thenReturn(List.of(addon));

        List<AddonResponse> result = addonAdminService.getAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().getName()).isEqualTo("Bluetooth");
        assertThat(result.getFirst().getPrice()).isEqualByComparingTo("10.00");
    }

    @Test
    void should_create_addon_when_request_is_valid() {
        AddonRequest request = new AddonRequest();
        request.setName(" Baby Seat ");
        request.setDescription(" Child seat ");
        request.setPrice(new BigDecimal("25"));
        request.setQuantity(5);
        request.setType(AddonType.DAILY);
        request.setIsActive(false);

        when(addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse("Baby Seat")).thenReturn(Optional.empty());
        when(addonRepository.save(any(Addon.class))).thenAnswer(invocation -> {
            Addon saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        AddonResponse result = addonAdminService.create(request);

        verify(addonRepository).save(addonCaptor.capture());
        Addon saved = addonCaptor.getValue();
        assertThat(saved.getName()).isEqualTo("Baby Seat");
        assertThat(saved.getDescription()).isEqualTo("Child seat");
        assertThat(saved.getPrice()).isEqualByComparingTo("25.00");
        assertThat(saved.getQuantity()).isEqualTo(5);
        assertThat(saved.getType()).isEqualTo(AddonType.DAILY);
        assertThat(saved.getIsActive()).isFalse();
        assertThat(saved.getInventoryManaged()).isTrue();
        assertThat(saved.getIsDeleted()).isFalse();
        assertThat(result.getName()).isEqualTo("Baby Seat");
    }

    @Test
    void should_throw_bad_request_when_creating_duplicate_addon() {
        AddonRequest request = new AddonRequest();
        request.setName("Bluetooth");

        when(addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse("Bluetooth")).thenReturn(Optional.of(addon));

        assertThatThrownBy(() -> addonAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An addon with this name already exists");

        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void should_throw_bad_request_when_creating_addon_with_blank_name() {
        AddonRequest request = new AddonRequest();
        request.setName("   ");

        assertThatThrownBy(() -> addonAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Addon name is required");
    }

    @Test
    void should_throw_bad_request_when_creating_addon_with_negative_price() {
        AddonRequest request = new AddonRequest();
        request.setName("Bluetooth");
        request.setPrice(new BigDecimal("-1"));

        when(addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse("Bluetooth")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addonAdminService.create(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Addon price cannot be negative");
    }

    @Test
    void should_update_addon_when_request_contains_new_values() {
        UUID id = addon.getId();
        AddonRequest request = new AddonRequest();
        request.setName(" Premium Bluetooth ");
        request.setDescription("   ");
        request.setPrice(new BigDecimal("12.5"));
        request.setQuantity(7);
        request.setType(AddonType.DAILY);
        request.setIsActive(false);

        when(addonRepository.findById(id)).thenReturn(Optional.of(addon));
        when(addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse("Premium Bluetooth")).thenReturn(Optional.empty());
        when(addonRepository.save(addon)).thenReturn(addon);

        AddonResponse result = addonAdminService.update(id, request);

        assertThat(addon.getName()).isEqualTo("Premium Bluetooth");
        assertThat(addon.getDescription()).isNull();
        assertThat(addon.getPrice()).isEqualByComparingTo("12.50");
        assertThat(addon.getQuantity()).isEqualTo(7);
        assertThat(addon.getType()).isEqualTo(AddonType.DAILY);
        assertThat(addon.getIsActive()).isFalse();
        assertThat(result.getPrice()).isEqualByComparingTo("12.50");
    }

    @Test
    void should_throw_not_found_when_updating_missing_addon() {
        UUID id = UUID.randomUUID();
        when(addonRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addonAdminService.update(id, new AddonRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Addon not found");
    }

    @Test
    void should_throw_bad_request_when_updating_addon_with_duplicate_name() {
        UUID id = addon.getId();
        Addon duplicate = new Addon();
        duplicate.setId(UUID.randomUUID());
        duplicate.setName("Bluetooth");
        AddonRequest request = new AddonRequest();
        request.setName("Bluetooth");

        when(addonRepository.findById(id)).thenReturn(Optional.of(addon));
        when(addonRepository.findFirstByNameIgnoreCaseAndIsDeletedFalse("Bluetooth")).thenReturn(Optional.of(duplicate));

        assertThatThrownBy(() -> addonAdminService.update(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("An addon with this name already exists");
    }

    @Test
    void should_throw_bad_request_when_updating_addon_with_negative_quantity() {
        UUID id = addon.getId();
        AddonRequest request = new AddonRequest();
        request.setQuantity(-1);

        when(addonRepository.findById(id)).thenReturn(Optional.of(addon));

        assertThatThrownBy(() -> addonAdminService.update(id, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Addon quantity cannot be negative");
    }

    @Test
    void should_delete_addon_when_it_has_no_booking_references() {
        UUID id = addon.getId();
        when(addonRepository.findById(id)).thenReturn(Optional.of(addon));
        when(bookingAddonRepository.existsByAddonId(id)).thenReturn(false);

        addonAdminService.delete(id);

        verify(addonRepository).delete(addon);
        verify(addonRepository, never()).save(any(Addon.class));
    }

    @Test
    void should_soft_delete_addon_when_it_has_booking_references() {
        UUID id = addon.getId();
        when(addonRepository.findById(id)).thenReturn(Optional.of(addon));
        when(bookingAddonRepository.existsByAddonId(id)).thenReturn(true);
        when(addonRepository.save(addon)).thenReturn(addon);

        addonAdminService.delete(id);

        assertThat(addon.getIsActive()).isFalse();
        assertThat(addon.getIsDeleted()).isTrue();
        verify(addonRepository).save(addon);
    }

    @Test
    void should_ensure_inventory_addons_when_defaults_missing() {
        Addon babySeat = new Addon();
        babySeat.setId(UUID.randomUUID());
        babySeat.setName("Baby Seat");
        babySeat.setPrice(new BigDecimal("25.00"));
        babySeat.setQuantity(0);
        babySeat.setType(AddonType.ONE_TIME);
        babySeat.setIsActive(true);
        babySeat.setInventoryManaged(true);
        babySeat.setIsDeleted(false);

        Addon bluetooth = new Addon();
        bluetooth.setId(UUID.randomUUID());
        bluetooth.setName("Bluetooth");
        bluetooth.setPrice(new BigDecimal("10.00"));
        bluetooth.setQuantity(0);
        bluetooth.setType(AddonType.ONE_TIME);
        bluetooth.setIsActive(true);
        bluetooth.setInventoryManaged(true);
        bluetooth.setIsDeleted(false);

        when(addonRepository.findFirstByNameIgnoreCase("Baby Seat")).thenReturn(Optional.empty());
        when(addonRepository.findFirstByNameIgnoreCase("Bluetooth")).thenReturn(Optional.empty());
        when(addonRepository.save(any(Addon.class)))
                .thenReturn(babySeat)
                .thenReturn(bluetooth);
        when(addonRepository.findAllByIsDeletedFalseOrderByNameAsc()).thenReturn(List.of(babySeat, bluetooth));

        List<AddonResponse> result = addonAdminService.ensureInventoryAddons();

        assertThat(result).hasSize(2);
        verify(addonRepository, org.mockito.Mockito.times(2)).save(addonCaptor.capture());
        assertThat(addonCaptor.getAllValues()).hasSize(2);
    }

    private AddonResponse toResponse(Addon addon) {
        AddonResponse response = new AddonResponse();
        response.setId(addon.getId());
        response.setName(addon.getName());
        response.setDescription(addon.getDescription());
        response.setPrice(addon.getPrice());
        response.setQuantity(addon.getQuantity());
        response.setType(addon.getType());
        response.setIsActive(addon.getIsActive());
        response.setInventoryManaged(addon.getInventoryManaged());
        response.setCreatedAt(addon.getCreatedAt());
        response.setUpdatedAt(addon.getUpdatedAt());
        return response;
    }
}
