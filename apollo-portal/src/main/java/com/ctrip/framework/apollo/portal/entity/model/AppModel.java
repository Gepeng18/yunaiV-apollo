package com.ctrip.framework.apollo.portal.entity.model;

import java.util.Set;

/**
 * App Model
 */
public class AppModel {

    /**
     * App 名
     */
    private String name;
    /**
     * App 编号
     */
    private String appId;
    /**
     * 部门编号
     */
    private String orgId;
    /**
     * 部门名
     */
    private String orgName;
    /**
     * 拥有人名
     */
    private String ownerName;
    /**
     * 管理员编号集合
     */
    private Set<String> admins;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getOrgId() {
        return orgId;
    }

    public void setOrgId(String orgId) {
        this.orgId = orgId;
    }

    public String getOrgName() {
        return orgName;
    }

    public void setOrgName(String orgName) {
        this.orgName = orgName;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public Set<String> getAdmins() {
        return admins;
    }

    public void setAdmins(Set<String> admins) {
        this.admins = admins;
    }
}
