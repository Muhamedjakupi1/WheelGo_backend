package com.wheelGo.model.user;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter
public class UserResponse {

    private UUID          id;
    private String        email;
    private String        role;
    private boolean       isActive;
    private boolean       emailVerified;
    private UUID          tenantId;
    private LocalDateTime createdAt;

    public static UserResponse from(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setEmail(u.getEmail());
        r.setRole(u.getRole());
        r.setActive(u.isActive());
        r.setEmailVerified(u.isEmailVerified());
        r.setTenantId(u.getTenant().getId());
        r.setCreatedAt(u.getCreatedAt());
        return r;
    }
}