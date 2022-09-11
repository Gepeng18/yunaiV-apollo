package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;

import java.util.List;

/**
 * Namespace BO ，提供 Namespace 的详细信息，包括配置明细和变更情况。
 */
public class NamespaceBO {

    /**
     * NamespaceDTO 对象
     */
    private NamespaceDTO baseInfo;
    /**
     * 配置变更（包括增删改）数量
     */
    private int itemModifiedCnt;
    /**
     * ItemBO 数组
     */
    private List<ItemBO> items;
    /**
     * {@link com.ctrip.framework.apollo.common.entity.AppNamespace#format}
     */
    private String format;
    /**
     * {@link com.ctrip.framework.apollo.common.entity.AppNamespace#isPublic}
     */
    private boolean isPublic;
    /**
     * {@link com.ctrip.framework.apollo.common.entity.AppNamespace#appId}
     */
    private String parentAppId;
    /**
     * {@link com.ctrip.framework.apollo.common.entity.AppNamespace#comment}
     */
    private String comment;

    public NamespaceDTO getBaseInfo() {
        return baseInfo;
    }

    public void setBaseInfo(NamespaceDTO baseInfo) {
        this.baseInfo = baseInfo;
    }

    public int getItemModifiedCnt() {
        return itemModifiedCnt;
    }

    public void setItemModifiedCnt(int itemModifiedCnt) {
        this.itemModifiedCnt = itemModifiedCnt;
    }

    public List<ItemBO> getItems() {
        return items;
    }

    public void setItems(List<ItemBO> items) {
        this.items = items;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public String getParentAppId() {
        return parentAppId;
    }

    public void setParentAppId(String parentAppId) {
        this.parentAppId = parentAppId;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

}
