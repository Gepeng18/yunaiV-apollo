package com.ctrip.framework.apollo.portal.entity.bo;

import com.ctrip.framework.apollo.common.dto.ItemDTO;

/**
 * Item BO
 */
public class ItemBO {

    /**
     * ItemDTO 对象
     */
    private ItemDTO item;
    /**
     * 是否修改（新增 or 更新）
     */
    private boolean isModified;
    /**
     * 是否删除
     */
    private boolean isDeleted;
    /**
     * 老值
     */
    private String oldValue;
    /**
     * 新值
     */
    private String newValue;

    public ItemDTO getItem() {
        return item;
    }

    public void setItem(ItemDTO item) {
        this.item = item;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }

    public boolean isModified() {
        return isModified;
    }

    public void setModified(boolean isModified) {
        this.isModified = isModified;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

}
