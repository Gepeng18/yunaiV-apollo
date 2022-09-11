package com.ctrip.framework.foundation.internals;

import com.google.common.base.Strings;

/**
 * 工具类
 */
public class Utils {

    public static boolean isBlank(String str) {
        return Strings.nullToEmpty(str).trim().isEmpty();
    }

    /**
     * @return 是否 windows 系统
     */
    public static boolean isOSWindows() {
        String osName = System.getProperty("os.name");
        if (Utils.isBlank(osName)) {
            return false;
        }
        return osName.startsWith("Windows");
    }

}