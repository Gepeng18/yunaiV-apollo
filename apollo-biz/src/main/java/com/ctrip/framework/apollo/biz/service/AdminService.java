package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.common.entity.App;
import com.ctrip.framework.apollo.core.ConfigConsts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin Service
 */
@Service
public class AdminService {

    @Autowired
    private AppService appService;
    @Autowired
    private AppNamespaceService appNamespaceService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private NamespaceService namespaceService;

    @Transactional
    public App createNewApp(App app) {
        // 保存 App 对象到数据库
        String createBy = app.getDataChangeCreatedBy();
        App createdApp = appService.save(app);
        String appId = createdApp.getAppId();
        // 创建 App 的默认命名空间 "application"
        appNamespaceService.createDefaultAppNamespace(appId, createBy);
        // 创建 App 的默认集群 "default"
        clusterService.createDefaultCluster(appId, createBy);
        // 创建 Cluster 的默认命名空间
        namespaceService.instanceOfAppNamespaces(appId, ConfigConsts.CLUSTER_NAME_DEFAULT, createBy);
        return app;
    }

}
