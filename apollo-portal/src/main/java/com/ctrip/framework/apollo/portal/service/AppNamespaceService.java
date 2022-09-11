package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.portal.repository.AppNamespaceRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * AppNamespace Service
 */
@Service
public class AppNamespaceService {

    @Autowired
    private UserInfoHolder userInfoHolder;
    @Autowired
    private AppNamespaceRepository appNamespaceRepository;
    @Autowired
    private RoleInitializationService roleInitializationService;
    @Autowired
    private AppService appService;

    /**
     * 公共的app ns,能被其它项目关联到的app ns
     */
    public List<AppNamespace> findPublicAppNamespaces() {
        return appNamespaceRepository.findByIsPublicTrue();
    }

    public AppNamespace findPublicAppNamespace(String namespaceName) {
        return appNamespaceRepository.findByNameAndIsPublic(namespaceName, true);
    }

    public AppNamespace findByAppIdAndName(String appId, String namespaceName) {
        return appNamespaceRepository.findByAppIdAndName(appId, namespaceName);
    }

    @Transactional
    public void createDefaultAppNamespace(String appId) {
        // 校验 `name` 在 App 下唯一
        if (!isAppNamespaceNameUnique(appId, ConfigConsts.NAMESPACE_APPLICATION)) {
            throw new BadRequestException(String.format("App already has application namespace. AppId = %s", appId));
        }
        // 创建 AppNamespace 对象
        AppNamespace appNs = new AppNamespace();
        appNs.setAppId(appId);
        appNs.setName(ConfigConsts.NAMESPACE_APPLICATION); // `application`
        appNs.setComment("default app namespace");
        appNs.setFormat(ConfigFileFormat.Properties.getValue());
        // 设置 AppNamespace 的创建和修改人为当前管理员
        String userId = userInfoHolder.getUser().getUserId();
        appNs.setDataChangeCreatedBy(userId);
        appNs.setDataChangeLastModifiedBy(userId);
        // 保存 AppNamespace 到数据库
        appNamespaceRepository.save(appNs);
    }

    public boolean isAppNamespaceNameUnique(String appId, String namespaceName) {
        Objects.requireNonNull(appId, "AppId must not be null");
        Objects.requireNonNull(namespaceName, "Namespace must not be null");
        return Objects.isNull(appNamespaceRepository.findByAppIdAndName(appId, namespaceName));
    }

    @Transactional
    public AppNamespace createAppNamespaceInLocal(AppNamespace appNamespace) {
        String appId = appNamespace.getAppId();
        // 校验对应的 App 是否存在。若不存在，抛出 BadRequestException 异常
        // add app org id as prefix
        App app = appService.load(appId);
        if (app == null) {
            throw new BadRequestException("App not exist. AppId = " + appId);
        }
        // 拼接 AppNamespace 的 `name` 属性。
        StringBuilder appNamespaceName = new StringBuilder();
        // add prefix postfix
        appNamespaceName
                .append(appNamespace.isPublic() ? app.getOrgId() + "." : "") // 公用类型，拼接组织编号
                .append(appNamespace.getName())
                .append(appNamespace.formatAsEnum() == ConfigFileFormat.Properties ? "" : "." + appNamespace.getFormat());
        appNamespace.setName(appNamespaceName.toString());
        // 设置 AppNamespace 的 `comment` 属性为空串，若为 null 。
        if (appNamespace.getComment() == null) {
            appNamespace.setComment("");
        }
        // 校验 AppNamespace 的 `format` 是否合法
        if (!ConfigFileFormat.isValidFormat(appNamespace.getFormat())) {
            throw new BadRequestException("Invalid namespace format. format must be properties、json、yaml、yml、xml");
        }
        // 设置 AppNamespace 的创建和修改人
        String operator = appNamespace.getDataChangeCreatedBy();
        if (StringUtils.isEmpty(operator)) {
            operator = userInfoHolder.getUser().getUserId(); // 当前登录管理员
            appNamespace.setDataChangeCreatedBy(operator);
        }
        appNamespace.setDataChangeLastModifiedBy(operator);
        // 公用类型，校验 `name` 在全局唯一
        // unique check
        if (appNamespace.isPublic() && findPublicAppNamespace(appNamespace.getName()) != null) {
            throw new BadRequestException(appNamespace.getName() + "已存在");
        }
        // 私有类型，校验 `name` 在 App 下唯一
        if (!appNamespace.isPublic() && appNamespaceRepository.findByAppIdAndName(appNamespace.getAppId(), appNamespace.getName()) != null) {
            throw new BadRequestException(appNamespace.getName() + "已存在");
        }
        // 保存 AppNamespace 到数据库
        AppNamespace createdAppNamespace = appNamespaceRepository.save(appNamespace);
        // 初始化 Namespace 的 Role 们
        roleInitializationService.initNamespaceRoles(appNamespace.getAppId(), appNamespace.getName(), operator);
        return createdAppNamespace;
    }

}