package com.xxedu.learning.support;

import com.xxedu.learning.common.enums.ClientType;
import com.xxedu.learning.security.LoginUser;
import com.xxedu.learning.security.PermissionCodes;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;

public final class TestAuth {

    private TestAuth() {
    }

    public static void login(String... permissions) {
        LoginUser user = new LoginUser(9L, "tester", ClientType.ADMIN, Set.of(permissions));
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    public static void loginOperator() {
        login(
                PermissionCodes.CONTENT_VIEW,
                PermissionCodes.CONTENT_CREATE,
                PermissionCodes.CONTENT_UPDATE,
                PermissionCodes.CONTENT_DELETE,
                PermissionCodes.CONTENT_PUBLISH,
                PermissionCodes.CONTENT_OFFLINE,
                PermissionCodes.CATEGORY_MANAGE,
                PermissionCodes.VIDEO_MANAGE,
                PermissionCodes.FILE_UPLOAD,
                PermissionCodes.DASHBOARD_VIEW,
                PermissionCodes.TOPIC_VIEW,
                PermissionCodes.TOPIC_CREATE,
                PermissionCodes.TOPIC_UPDATE,
                PermissionCodes.TOPIC_DELETE,
                PermissionCodes.TOPIC_PUBLISH,
                PermissionCodes.TOPIC_OFFLINE,
                PermissionCodes.TOPIC_CONTENT_MANAGE,
                PermissionCodes.HOME_OPERATION_VIEW,
                PermissionCodes.HOME_OPERATION_MANAGE);
    }
}
