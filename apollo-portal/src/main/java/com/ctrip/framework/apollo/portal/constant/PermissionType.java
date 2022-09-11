package com.ctrip.framework.apollo.portal.constant;

/**
 * 操作类型
 */
public interface PermissionType {

    // ========== APP level permission ==========
    String CREATE_NAMESPACE = "CreateNamespace"; // 创建 Namespace
    String CREATE_CLUSTER = "CreateCluster"; // 创建 Cluster
    String ASSIGN_ROLE = "AssignRole"; // 分配用户权限的权限

    // ========== namespace level permission =========
    String MODIFY_NAMESPACE = "ModifyNamespace"; // 修改 Namespace
    String RELEASE_NAMESPACE = "ReleaseNamespace"; // 发布 Namespace

}