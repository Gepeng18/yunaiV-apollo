package com.ctrip.framework.apollo.biz.grayReleaseRule;

import com.ctrip.framework.apollo.common.dto.GrayReleaseRuleItemDTO;

import java.util.Set;

/**
 * {@link com.ctrip.framework.apollo.biz.entity.GrayReleaseRule} 缓存对象
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRuleCache {

    private long ruleId;

    // 缺少 appId
    // 缺少 clusterName

    private String namespaceName;
    private String branchName;
    private Set<GrayReleaseRuleItemDTO> ruleItems;
    private long releaseId;
    private int branchStatus;
    /**
     * 加载版本
     */
    private long loadVersion;

    public GrayReleaseRuleCache(long ruleId, String branchName, String namespaceName, long
            releaseId, int branchStatus, long loadVersion, Set<GrayReleaseRuleItemDTO> ruleItems) {
        this.ruleId = ruleId;
        this.branchName = branchName;
        this.namespaceName = namespaceName;
        this.releaseId = releaseId;
        this.branchStatus = branchStatus;
        this.loadVersion = loadVersion;
        this.ruleItems = ruleItems;
    }

    // 匹配 clientAppId + clientIp
    public boolean matches(String clientAppId, String clientIp) {
        for (GrayReleaseRuleItemDTO ruleItem : ruleItems) {
            if (ruleItem.matches(clientAppId, clientIp)) {
                return true;
            }
        }
        return false;
    }

    public long getRuleId() {
        return ruleId;
    }

    public Set<GrayReleaseRuleItemDTO> getRuleItems() {
        return ruleItems;
    }

    public String getBranchName() {
        return branchName;
    }

    public int getBranchStatus() {
        return branchStatus;
    }

    public long getReleaseId() {
        return releaseId;
    }

    public long getLoadVersion() {
        return loadVersion;
    }

    public void setLoadVersion(long loadVersion) {
        this.loadVersion = loadVersion;
    }

    public String getNamespaceName() {
        return namespaceName;
    }

}
