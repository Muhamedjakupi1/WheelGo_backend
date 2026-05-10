package com.wheelGo.tools;

import com.wheelGo.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    public static CustomUserPrincipal getCurrentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal;
        }
        return null;
    }

    public static UUID getCurrentUserId(){
        CustomUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getUserId() : null;
    }

    public static UUID getCurrentTenantId() {
        CustomUserPrincipal principal = getCurrentPrincipal();
        return principal != null ? principal.getTenantId() : null;
    }
}
