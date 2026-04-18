package com.wheelGo.model.user;

import com.wheelGo.model.enums.Role;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class UserResponse {

    private UUID          id;
    private String        email;
    private Role          role;
    private boolean       isActive;
    private boolean       emailVerified;
    private UUID          tenantId;
    private boolean       isImpersonate;
    private LocalDateTime createdAt;
}