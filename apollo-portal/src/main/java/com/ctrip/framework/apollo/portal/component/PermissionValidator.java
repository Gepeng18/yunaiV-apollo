package com.ctrip.framework.apollo.portal.component;

import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.constant.PermissionType;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 权限校验器
 */
@Component("permissionValidator")
public class PermissionValidator {

    @Autowired
    private UserInfoHolder userInfoHolder;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private PortalConfig portalConfig;

    // ========== Namespace 级别 ==========

    public boolean hasModifyNamespacePermission(String appId, String namespaceName) {
        return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
                PermissionType.MODIFY_NAMESPACE,
                RoleUtils.buildNamespaceTargetId(appId, namespaceName));
    }

    public boolean hasReleaseNamespacePermission(String appId, String namespaceName) {
        return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
                PermissionType.RELEASE_NAMESPACE,
                RoleUtils.buildNamespaceTargetId(appId, namespaceName));
    }

    public boolean hasDeleteNamespacePermission(String appId) {
        return hasAssignRolePermission(appId) || isSuperAdmin();
    }

    public boolean hasOperateNamespacePermission(String appId, String namespaceName) {
        return hasModifyNamespacePermission(appId, namespaceName) || hasReleaseNamespacePermission(appId, namespaceName);
    }

    // ========== App 级别 ==========

    public boolean hasAssignRolePermission(String appId) {
        return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
                PermissionType.ASSIGN_ROLE,
                appId);
    }

    public boolean hasCreateNamespacePermission(String appId) {
        return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
                PermissionType.CREATE_NAMESPACE,
                appId);
    }

    public boolean hasCreateAppNamespacePermission(String appId, AppNamespace appNamespace) {
        boolean isPublicAppNamespace = appNamespace.isPublic();
        // 若满足如下任一条件：
        // 1. 公开类型的 AppNamespace 。
        // 2. 私有类型的 AppNamespace ，并且允许 App 管理员创建私有类型的 AppNamespace 。
        if (portalConfig.canAppAdminCreatePrivateNamespace() || isPublicAppNamespace) {
            return hasCreateNamespacePermission(appId);
        }
        // 超管
        return isSuperAdmin();
    }

    public boolean hasCreateClusterPermission(String appId) {
        return rolePermissionService.userHasPermission(userInfoHolder.getUser().getUserId(),
                PermissionType.CREATE_CLUSTER,
                appId);
    }

    public boolean isAppAdmin(String appId) {
        return isSuperAdmin() || hasAssignRolePermission(appId);
    }

    // ========== 超管 级别 ==========

    public boolean isSuperAdmin() {
        return rolePermissionService.isSuperAdmin(userInfoHolder.getUser().getUserId());
    }

}
