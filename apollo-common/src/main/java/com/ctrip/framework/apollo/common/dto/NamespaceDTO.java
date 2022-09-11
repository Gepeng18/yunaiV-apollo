package com.ctrip.framework.apollo.common.dto;

/**
 * Namespace DTO
 */
public class NamespaceDTO extends BaseDTO {

    private long id;
    /**
     * App 编号
     */
    private String appId;
    /**
     * Cluster 名字
     */
    private String clusterName;
    /**
     * Namespace 名字
     */
    private String namespaceName;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAppId() {
        return appId;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setClusterName(String clusterName) {
        this.clusterName = clusterName;
    }

    public void setNamespaceName(String namespaceName) {
        this.namespaceName = namespaceName;
    }
}