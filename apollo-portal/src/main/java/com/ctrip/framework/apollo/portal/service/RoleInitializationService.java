package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.entity.App;

/**
 * 角色初始化 Service 接口
 */
public interface RoleInitializationService {

    /**
     * 初始化 App 级的 Role
     */
    void initAppRoles(App app);

    /**
     * 初始化 Namespace 级的 Role
     */
    void initNamespaceRoles(String appId, String namespaceName, String operator);

}