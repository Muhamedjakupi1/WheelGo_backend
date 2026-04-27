package com.wheelGo.tools;

import com.wheelGo.security.CustomUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public class SecurityUtils {

    public static UUID getCurrentUserId(){
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if(auth !=null && auth.getPrincipal() instanceof CustomUserPrincipal principal){
            return principal.getUserId();
        }
        return null;
    }
}
