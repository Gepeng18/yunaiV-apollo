package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigFile;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.enums.ConfigFileFormat;
import com.ctrip.framework.apollo.internals.*;
import com.ctrip.framework.apollo.util.ConfigUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认配置工厂实现类
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactory implements ConfigFactory {

    private static final Logger logger = LoggerFactory.getLogger(DefaultConfigFactory.class);
    private ConfigUtil m_configUtil;

    public DefaultConfigFactory() {
        m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
    }

    @Override
    public Config create(String namespace) {
        // 创建 DefaultConfig 对象（入参是 ConfigRepository 对象）
        // 这个ConfigRepository能从远程拉取properties配置，并且如果远程挂了，还能从持久化的本地文件中读取，然后赋值给局部变量properties
        return new DefaultConfig(namespace, createLocalConfigRepository(namespace));
    }

    @Override
    public ConfigFile createConfigFile(String namespace, ConfigFileFormat configFileFormat) {
        // 创建 ConfigRepository 对象
        ConfigRepository configRepository = createLocalConfigRepository(namespace);
        // 创建对应的 ConfigFile 对象
        switch (configFileFormat) {
            case Properties:
                return new PropertiesConfigFile(namespace, configRepository);
            case XML:
                return new XmlConfigFile(namespace, configRepository);
            case JSON:
                return new JsonConfigFile(namespace, configRepository);
            case YAML:
                return new YamlConfigFile(namespace, configRepository);
            case YML:
                return new YmlConfigFile(namespace, configRepository);
        }
        return null;
    }

    LocalFileConfigRepository createLocalConfigRepository(String namespace) {
        // 本地模式，使用 LocalFileConfigRepository 对象
        if (m_configUtil.isInLocalMode()) {
            logger.warn("==== Apollo is in local mode! Won't pull configs from remote server for namespace {} ! ====", namespace);
            return new LocalFileConfigRepository(namespace);
        }
        // 非本地模式，使用 LocalFileConfigRepository(RemoteConfigRepository)
        // 这个对象非常重要，它是一个本地文件配置仓库，但是传入了一个参数，是远程配置仓库，什么意思呢？
        // 即定义了，这个model能从远程拉取properties配置，并且如果远程挂了，还能从持久化的本地文件中读取，然后赋值给局部变量properties
        return new LocalFileConfigRepository(namespace, createRemoteConfigRepository(namespace));
    }

    RemoteConfigRepository createRemoteConfigRepository(String namespace) {
        return new RemoteConfigRepository(namespace);
    }

}