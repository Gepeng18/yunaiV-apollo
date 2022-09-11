package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Permission;
import com.ctrip.framework.apollo.portal.entity.po.Role;

import java.util.Set;

/**
 * RolePermissionService 接口，提供 Role、UserRole、Permission、UserPermission 相关的操作
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface RolePermissionService {

    // ========== Role 相关 ==========
    /**
     * Create role with permissions, note that role name should be unique
     */
    Role createRoleWithPermissions(Role role, Set<Long> permissionIds);
    /**
     * Find role by role name, note that roleName should be unique
     */
    Role findRoleByRoleName(String roleName);

    // ========== UserRole 相关 ==========
    /**
     * Assign role to users
     *
     * @return the users assigned roles
     */
    Set<String> assignRoleToUsers(String roleName, Set<String> userIds, String operatorUserId);
    /**
     * Remove role from users
     */
    void removeRoleFromUsers(String roleName, Set<String> userIds, String operatorUserId);
    /**
     * Query users with role
     */
    Set<UserInfo> queryUsersWithRole(String roleName);

    // ========== UserPermission 相关 ==========
    /**
     * Check whether user has the permission
     */
    boolean userHasPermission(String userId, String permissionType, String targetId);
    /**
     * 校验是否为超级管理员
     */
    boolean isSuperAdmin(String userId);

    // ========== Permission 相关 ==========
    /**
     * Create permission, note that permissionType + targetId should be unique
     */
    Permission createPermission(Permission permission);
    /**
     * Create permissions, note that permissionType + targetId should be unique
     */
    Set<Permission> createPermissions(Set<Permission> permissions);

}