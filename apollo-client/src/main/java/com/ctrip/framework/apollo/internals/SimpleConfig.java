package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Function;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 简单的 Config 实现类，从目前代码看来下，用于单元测试。相比 DefaultConfig 来说，少一些特性，大体是相同的。
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class SimpleConfig extends AbstractConfig implements RepositoryChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(SimpleConfig.class);

    /**
     * Namespace 的名字
     */
    private final String m_namespace;
    /**
     * 配置 Properties 的缓存
     * <p>
     * 相比 {@link DefaultConfig#m_configProperties} 少了一个 AtomicReference
     */
    private volatile Properties m_configProperties;
    /**
     * 配置 Repository
     */
    private final ConfigRepository m_configRepository;

    /**
     * Constructor.
     *
     * @param namespace        the namespace for this config instance
     * @param configRepository the config repository for this config instance
     */
    public SimpleConfig(String namespace, ConfigRepository configRepository) {
        m_namespace = namespace;
        m_configRepository = configRepository;
        this.initialize();
    }

    private void initialize() {
        try {
            // 初始化 m_configProperties
            m_configProperties = m_configRepository.getConfig();
        } catch (Throwable ex) {
            Tracer.logError(ex);
            logger.warn("Init Apollo Simple Config failed - namespace: {}, reason: {}", m_namespace, ExceptionUtil.getDetailMessage(ex));
        } finally {
            //register the change listener no matter config repository is working or not
            //so that whenever config repository is recovered, config could get changed
            // 注册到 ConfigRepository 中，从而实现每次配置发生变更时，更新配置缓存 `m_configProperties` 。
            m_configRepository.addChangeListener(this);
        }
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        if (m_configProperties == null) {
            logger.warn("Could not load config from Apollo, always return default value!");
            return defaultValue;
        }
        // 从缓存 Properties 获得属性
        return this.m_configProperties.getProperty(key, defaultValue);
    }

    @Override
    public Set<String> getPropertyNames() {
        // 若为空，返回空集合
        if (m_configProperties == null) {
            return Collections.emptySet();
        }
        return m_configProperties.stringPropertyNames();
    }

    @Override
    public synchronized void onRepositoryChange(String namespace, Properties newProperties) {
        // 忽略，若未变更
        if (newProperties.equals(m_configProperties)) {
            return;
        }
        // 读取新的 Properties 对象
        Properties newConfigProperties = new Properties();
        newConfigProperties.putAll(newProperties);

        // 计算配置变更集合
        List<ConfigChange> changes = calcPropertyChanges(namespace, m_configProperties, newConfigProperties);
        Map<String, ConfigChange> changeMap = Maps.uniqueIndex(changes, new Function<ConfigChange, String>() {
            @Override
            public String apply(ConfigChange input) {
                return input.getPropertyName();
            }
        });

        // 更新到 `m_configProperties` 中
        m_configProperties = newConfigProperties;
        // 清空 Cache 缓存
        clearConfigCache();

        // 通知监听器们
        this.fireConfigChange(new ConfigChangeEvent(m_namespace, changeMap));

        Tracer.logEvent("Apollo.Client.ConfigChanges", m_namespace);
    }
}
