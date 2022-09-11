package com.ctrip.framework.apollo.portal.spi;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Portal 页面如果长时间不刷新，登录信息会过期。通过此接口来刷新登录信息。
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface SsoHeartbeatHandler {

    void doHeartbeat(HttpServletRequest request, HttpServletResponse response);

}