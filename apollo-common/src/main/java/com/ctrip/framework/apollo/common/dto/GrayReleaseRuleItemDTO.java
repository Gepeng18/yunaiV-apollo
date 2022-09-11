package com.ctrip.framework.apollo.common.dto;

import com.google.common.collect.Sets;

import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * GrayRelease 规则项 DTO
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class GrayReleaseRuleItemDTO {

    public static final String ALL_IP = "*";

    /**
     * 客户端 App 编号
     */
    private String clientAppId;
    /**
     * 客户端 IP 集合
     */
    private Set<String> clientIpList;

    public GrayReleaseRuleItemDTO(String clientAppId) {
        this(clientAppId, Sets.newHashSet());
    }

    public GrayReleaseRuleItemDTO(String clientAppId, Set<String> clientIpList) {
        this.clientAppId = clientAppId;
        this.clientIpList = clientIpList;
    }

    public String getClientAppId() {
        return clientAppId;
    }

    public Set<String> getClientIpList() {
        return clientIpList;
    }

    // 匹配方法 BEGIN

    public boolean matches(String clientAppId, String clientIp) {
        return appIdMatches(clientAppId) && ipMatches(clientIp);
    }

    private boolean appIdMatches(String clientAppId) {
        return this.clientAppId.equals(clientAppId);
    }

    private boolean ipMatches(String clientIp) {
        return this.clientIpList.contains(ALL_IP) || clientIpList.contains(clientIp);
    }

    // 匹配方法 END

    @Override
    public String toString() {
        return toStringHelper(this).add("clientAppId", clientAppId)
                .add("clientIpList", clientIpList).toString();
    }

}
