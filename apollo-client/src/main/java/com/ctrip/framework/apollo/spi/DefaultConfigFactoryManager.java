package com.ctrip.framework.apollo.spi;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.google.common.collect.Maps;

import java.util.Map;

/**
 * 默认 ConfigFactory 管理器实现类
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class DefaultConfigFactoryManager implements ConfigFactoryManager {

    private ConfigRegistry m_registry;
    /**
     * ConfigFactory 对象的缓存
     */
    private Map<String, ConfigFactory> m_factories = Maps.newConcurrentMap();

    public DefaultConfigFactoryManager() {
        m_registry = ApolloInjector.getInstance(ConfigRegistry.class);
    }

    @Override
    public ConfigFactory getFactory(String namespace) {
        // step 1: check hacked factory
        // 从 ConfigRegistry 中，获得 ConfigFactory 对象
        ConfigFactory factory = m_registry.getFactory(namespace);
        if (factory != null) {
            return factory;
        }

        // step 2: check cache 从缓存中，获得 ConfigFactory 对象
        factory = m_factories.get(namespace);
        if (factory != null) {
            return factory;
        }

        // step 3: check declared config factory 从 ApolloInjector 中，获得指定 Namespace 的 ConfigFactory 对象
        factory = ApolloInjector.getInstance(ConfigFactory.class, namespace);
        if (factory != null) {
            return factory;
        }

        // step 4: check default config factory
        // 从 ApolloInjector 中，获得默认的 ConfigFactory 对象
        factory = ApolloInjector.getInstance(ConfigFactory.class);

        // 更新到缓存中
        m_factories.put(namespace, factory);

        // factory should not be null
        return factory;
    }

}