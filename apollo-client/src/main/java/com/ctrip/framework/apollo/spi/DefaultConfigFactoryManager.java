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

    /**
     * 1、先获取工厂（工厂是通过registry对象拿到的，registry对象是通过基于 Guice 进行依赖注入对象的 getInstance(ConfigRegistry)拿到的）
     * 2、如果通过registry对象拿不到工厂，则看看本地工厂缓存有没有
     * 3、本地工厂缓存没有，则通过getInstance(ConfigFactory, namepsace)拿
     * 4、还没有，则通过getInstance(ConfigFactory)拿默认的ConfigFactory 对象
     */
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