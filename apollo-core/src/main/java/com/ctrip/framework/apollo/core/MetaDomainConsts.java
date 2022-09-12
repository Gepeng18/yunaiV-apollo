package com.ctrip.framework.apollo.core;

import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.core.utils.ResourceUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The meta domain will load the meta server from System environment first, if not exist, will load
 * from apollo-env.properties. If neither exists, will load the default meta url.
 * <p>
 * Currently, apollo supports local/dev/fat/uat/lpt/pro environments.
 */
public class MetaDomainConsts {

    private static Map<Env, Object> domains = new HashMap<>();

    public static final String DEFAULT_META_URL = "http://config.local";

    static {
        // 读取配置文件到 Properties 中
        Properties prop = new Properties();
        prop = ResourceUtils.readConfigFile("apollo-env.properties", prop);
        // 获得系统 Properties
        Properties env = System.getProperties();
        // 添加到 domains 中
        // 优先级，env > prop
        domains.put(Env.LOCAL, env.getProperty("local_meta", prop.getProperty("local.meta", DEFAULT_META_URL)));
        domains.put(Env.DEV, env.getProperty("dev_meta", prop.getProperty("dev.meta", DEFAULT_META_URL)));
        domains.put(Env.FAT, env.getProperty("fat_meta", prop.getProperty("fat.meta", DEFAULT_META_URL)));
        domains.put(Env.UAT, env.getProperty("uat_meta", prop.getProperty("uat.meta", DEFAULT_META_URL)));
        domains.put(Env.LPT, env.getProperty("lpt_meta", prop.getProperty("lpt.meta", DEFAULT_META_URL)));
        domains.put(Env.PRO, env.getProperty("pro_meta", prop.getProperty("pro.meta", DEFAULT_META_URL)));
    }

    /**
     * 域名应该配置在 System.getProperties() 中，然后通过环境对应的字段取出来
     */
    public static String getDomain(Env env) {
        return String.valueOf(domains.get(env));
    }

}