package com.ctrip.framework.apollo.common.entity;


import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * App Namespace 实体
 */
@Entity
@Table(name = "AppNamespace")
@SQLDelete(sql = "Update AppNamespace set isDeleted = 1 where id = ?")
@Where(clause = "isDeleted = 0")
public class AppNamespace extends BaseEntity {

    /**
     * 名字
     */
    @Column(name = "Name", nullable = false)
    private String name;
    /**
     * App 编号
     */
    @Column(name = "AppId", nullable = false)
    private String appId;
    /**
     * 格式
     *
     * 参见 {@link ConfigFileFormat}
     */
    @Column(name = "Format", nullable = false)
    private String format;
    /**
     * 是否公用的
     */
    @Column(name = "IsPublic", columnDefinition = "Bit default '0'")
    private boolean isPublic = false;
    /**
     * 备注
     */
    @Column(name = "Comment")
    private String comment;

    public String getAppId() {
        return appId;
    }

    public String getComment() {
        return comment;
    }

    public String getName() {
        return name;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public ConfigFileFormat formatAsEnum() {
        return ConfigFileFormat.fromString(this.format);
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String toString() {
        return toStringHelper().add("name", name).add("appId", appId).add("comment", comment)
                .add("format", format).add("isPublic", isPublic).toString();
    }
}