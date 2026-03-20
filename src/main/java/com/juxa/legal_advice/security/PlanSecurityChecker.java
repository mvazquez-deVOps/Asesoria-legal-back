package com.juxa.legal_advice.security;

import com.juxa.legal_advice.config.JuxaPlanDef;
import com.juxa.legal_advice.model.UserEntity;
import com.juxa.legal_advice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("planChecker")
public class PlanSecurityChecker {

    @Autowired
    private UserService userService;

    public boolean canUploadAudio() {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        return JuxaPlanDef.fromString(user.getSubscriptionPlan()).isCanUploadAudio();
    }

    public boolean canUploadVideo() {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        return JuxaPlanDef.fromString(user.getSubscriptionPlan()).isCanUploadVideo();
    }

    public boolean hasFullHistory() {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        return JuxaPlanDef.fromString(user.getSubscriptionPlan()).isHasFullHistory();
    }

    // Por si necesitas bloquear algo exclusivo del plan más caro
    public boolean isElite() {
        UserEntity user = userService.getCurrentAuthenticatedUser();
        return user.getSubscriptionPlan().equalsIgnoreCase("premium_elite");
    }
}