package com.wheelGo.model.user_settings;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class UserSettingsPasswordUpdateResponse {
    private UUID userId;
    private boolean passwordChanged;
    private LocalDateTime updatedAt;
}
