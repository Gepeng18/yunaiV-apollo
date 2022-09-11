package com.ctrip.framework.apollo.portal.spi.springsecurity;

import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.security.Principal;

/**
 * 基于 Spring Security 的 UserInfoHolder 实现类
 */
public class SpringSecurityUserInfoHolder implements UserInfoHolder {

    @Override
    public UserInfo getUser() {
        // 创建 UserInfo 对象，设置 `username` 到 `UserInfo.userId` 中。
        UserInfo userInfo = new UserInfo();
        userInfo.setUserId(getCurrentUsername());
        return userInfo;
    }

    /**
     * @return username
     */
    private String getCurrentUsername() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserDetails) {
            return ((UserDetails) principal).getUsername();
        }
        if (principal instanceof Principal) {
            return ((Principal) principal).getName();
        }
        return String.valueOf(principal);
    }

}