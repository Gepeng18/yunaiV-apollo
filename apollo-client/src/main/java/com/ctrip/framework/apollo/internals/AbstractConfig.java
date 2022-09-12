package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.utils.ApolloThreadFactory;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.function.Functions;
import com.ctrip.framework.apollo.util.parser.Parsers;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Config 抽象类
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public abstract class AbstractConfig implements Config {

    private static final Logger logger = LoggerFactory.getLogger(AbstractConfig.class);

    /**
     * ExecutorService 对象，用于配置变化时，异步通知 ConfigChangeListener 监听器们
     *
     * 静态属性，所有 Config 共享该线程池。
     */
    private static ExecutorService m_executorService;

    /**
     * ConfigChangeListener 集合
     */
    private List<ConfigChangeListener> m_listeners = Lists.newCopyOnWriteArrayList();
    private ConfigUtil m_configUtil;

    /**
     * 这些缓存是跟随 DefaultConfig 的
     */
    private volatile Cache<String, Integer> m_integerCache;
    private volatile Cache<String, Long> m_longCache;
    private volatile Cache<String, Short> m_shortCache;
    private volatile Cache<String, Float> m_floatCache;
    private volatile Cache<String, Double> m_doubleCache;
    private volatile Cache<String, Byte> m_byteCache;
    private volatile Cache<String, Boolean> m_booleanCache;
    private volatile Cache<String, Date> m_dateCache;
    private volatile Cache<String, Long> m_durationCache;
    /**
     * 数组属性 Cache Map
     *
     * KEY：分隔符
     * KEY2：属性建
     */
    private Map<String, Cache<String, String[]>> m_arrayCache; // 并发 Map
    /**
     * 上述 Cache 对象集合
     */
    private List<Cache> allCaches;
    /**
     * 缓存版本号，用于解决更新缓存可能存在的并发问题。详细见 {@link #getValueAndStoreToCache(String, Function, Cache, Object)} 方法
     */
    private AtomicLong m_configVersion; //indicate config version

    static {
        m_executorService = Executors.newCachedThreadPool(ApolloThreadFactory.create("Config", true));
    }

    public AbstractConfig() {
        m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        m_configVersion = new AtomicLong();
        m_arrayCache = Maps.newConcurrentMap();
        allCaches = Lists.newArrayList();
    }

    @Override
    public void addChangeListener(ConfigChangeListener listener) {
        if (!m_listeners.contains(listener)) {
            m_listeners.add(listener);
        }
    }

    @Override
    public Integer getIntProperty(String key, Integer defaultValue) {
        try {
            // 1、本地缓存没有，初始化本地缓存
            if (m_integerCache == null) {
                synchronized (this) {
                    if (m_integerCache == null) {
                        m_integerCache = newCache();
                    }
                }
            }
            // 2、从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_INT_FUNCTION, m_integerCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getIntProperty for %s failed, return default value %d", key,
                            defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Long getLongProperty(String key, Long defaultValue) {
        try {
            // 初始化缓存
            if (m_longCache == null) {
                synchronized (this) {
                    if (m_longCache == null) {
                        m_longCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_LONG_FUNCTION, m_longCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getLongProperty for %s failed, return default value %d", key,
                            defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Short getShortProperty(String key, Short defaultValue) {
        try {
            // 初始化缓存
            if (m_shortCache == null) {
                synchronized (this) {
                    if (m_shortCache == null) {
                        m_shortCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_SHORT_FUNCTION, m_shortCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(String.format("getShortProperty for %s failed, return default value %d", key, defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Float getFloatProperty(String key, Float defaultValue) {
        try {
            // 初始化缓存
            if (m_floatCache == null) {
                synchronized (this) {
                    if (m_floatCache == null) {
                        m_floatCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_FLOAT_FUNCTION, m_floatCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(String.format("getFloatProperty for %s failed, return default value %f", key, defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Double getDoubleProperty(String key, Double defaultValue) {
        try {
            // 初始化缓存
            if (m_doubleCache == null) {
                synchronized (this) {
                    if (m_doubleCache == null) {
                        m_doubleCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_DOUBLE_FUNCTION, m_doubleCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(String.format("getDoubleProperty for %s failed, return default value %f", key, defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Byte getByteProperty(String key, Byte defaultValue) {
        try {
            // 初始化缓存
            if (m_byteCache == null) {
                synchronized (this) {
                    if (m_byteCache == null) {
                        m_byteCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_BYTE_FUNCTION, m_byteCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getByteProperty for %s failed, return default value %d", key,
                            defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public Boolean getBooleanProperty(String key, Boolean defaultValue) {
        try {
            // 初始化缓存
            if (m_booleanCache == null) {
                synchronized (this) {
                    if (m_booleanCache == null) {
                        m_booleanCache = newCache();
                    }
                }
            }
            // 从缓存中，读取属性值
            return getValueFromCache(key, Functions.TO_BOOLEAN_FUNCTION, m_booleanCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getBooleanProperty for %s failed, return default value %b", key,
                            defaultValue), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public String[] getArrayProperty(String key, final String delimiter, String[] defaultValue) {
        try {
            // 初始化缓存
            if (!m_arrayCache.containsKey(delimiter)) {
                synchronized (this) {
                    if (!m_arrayCache.containsKey(delimiter)) {
                        m_arrayCache.put(delimiter, this.<String[]>newCache());
                    }
                }
            }
            // 获得对应的 Cache 对象
            Cache<String, String[]> cache = m_arrayCache.get(delimiter);
            // 获得属性值
            String[] result = cache.getIfPresent(key);
            if (result != null) {
                return result;
            }
            // 从缓存中，读取属性值
            return getValueAndStoreToCache(key, new Function<String, String[]>() {
                @Override
                public String[] apply(String input) {
                    return input.split(delimiter);
                }
            }, cache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getArrayProperty for %s failed, return default value", key), ex));
        }
        // 默认值
        return defaultValue;
    }

    @Override
    public <T extends Enum<T>> T getEnumProperty(String key, Class<T> enumType, T defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Enum.valueOf(enumType, value);
            }
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getEnumProperty for %s failed, return default value %s", key,
                            defaultValue), ex));
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, Date defaultValue) {
        try {
            if (m_dateCache == null) {
                synchronized (this) {
                    if (m_dateCache == null) {
                        m_dateCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_DATE_FUNCTION, m_dateCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getDateProperty for %s failed, return default value %s", key,
                            defaultValue), ex));
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, String format, Date defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Parsers.forDate().parse(value, format);
            }
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getDateProperty for %s failed, return default value %s", key,
                            defaultValue), ex));
        }

        return defaultValue;
    }

    @Override
    public Date getDateProperty(String key, String format, Locale locale, Date defaultValue) {
        try {
            String value = getProperty(key, null);

            if (value != null) {
                return Parsers.forDate().parse(value, format, locale);
            }
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getDateProperty for %s failed, return default value %s", key,
                            defaultValue), ex));
        }

        return defaultValue;
    }

    @Override
    public long getDurationProperty(String key, long defaultValue) {
        try {
            if (m_durationCache == null) {
                synchronized (this) {
                    if (m_durationCache == null) {
                        m_durationCache = newCache();
                    }
                }
            }

            return getValueFromCache(key, Functions.TO_DURATION_FUNCTION, m_durationCache, defaultValue);
        } catch (Throwable ex) {
            Tracer.logError(new ApolloConfigException(
                    String.format("getDurationProperty for %s failed, return default value %d", key,
                            defaultValue), ex));
        }

        return defaultValue;
    }

    private <T> T getValueFromCache(String key, Function<String, T> parser, Cache<String, T> cache, T defaultValue) {
        // 1、从缓存中读取属性值，若存在，则返回
        T result = cache.getIfPresent(key);
        if (result != null) {
            return result;
        }
        // 2、本地缓存中没有值，则调用getProperty获取值，并且获取值后更新到缓存
        return getValueAndStoreToCache(key, parser, cache, defaultValue);
    }

    private <T> T getValueAndStoreToCache(String key, Function<String, T> parser, Cache<String, T> cache, T defaultValue) {
        // 获得当前版本号
        long currentConfigVersion = m_configVersion.get();
        // 1、调用getProperty获得属性值（其实就是根据remote or localFile获取该属性值）
        String value = getProperty(key, null);
        // 若获得到属性，返回该属性值
        if (value != null) {
            // 2、使用映射函数解析属性值
            T result = parser.apply(value);
            // 若解析成功
            if (result != null) {
                // do 3、若版本号未变化，则更新到缓存，从而解决并发的问题。
                // 猜测这里是为了解决，调用getProperty的间隙，有人设值了？double check不行嘛
                // 还不是，看到m_configVersion在清除缓存的时候，才加1，也就是说，如果在getProperty的时间间隙，缓存被清了，就不设置到缓存里了，为啥
                //
                synchronized (this) {
                    if (m_configVersion.get() == currentConfigVersion) {
                        cache.put(key, result);
                    }
                }
                // 4、返回属性值
                return result;
            }
        }
        // 5、获得不到属性值，返回默认值
        return defaultValue;
    }

    private <T> Cache<String, T> newCache() {
        // 创建 Cache 对象
        Cache<String, T> cache = CacheBuilder.newBuilder()
                .maximumSize(m_configUtil.getMaxConfigCacheSize()) // 500
                .expireAfterAccess(m_configUtil.getConfigCacheExpireTime(), // 1 分钟
                        m_configUtil.getConfigCacheExpireTimeUnit())
                .build();
        // 添加到 Cache 集合
        allCaches.add(cache);
        return cache;
    }

    /**
     * Clear config cache
     */
    protected void clearConfigCache() {
        synchronized (this) {
            // 过期缓存
            for (Cache c : allCaches) {
                if (c != null) {
                    c.invalidateAll();
                }
            }
            // 新增版本号
            m_configVersion.incrementAndGet();
        }
    }

    protected void fireConfigChange(final ConfigChangeEvent changeEvent) {
        // 缓存 ConfigChangeListener 数组
        for (final ConfigChangeListener listener : m_listeners) {
            m_executorService.submit(new Runnable() {
                @Override
                public void run() {
                    String listenerName = listener.getClass().getName();
                    Transaction transaction = Tracer.newTransaction("Apollo.ConfigChangeListener", listenerName);
                    try {
                        // 通知监听器
                        listener.onChange(changeEvent);
                        transaction.setStatus(Transaction.SUCCESS);
                    } catch (Throwable ex) {
                        transaction.setStatus(ex);
                        Tracer.logError(ex);
                        logger.error("Failed to invoke config change listener {}", listenerName, ex);
                    } finally {
                        transaction.complete();
                    }
                }
            });
        }
    }

    // 计算配置变更集合
    List<ConfigChange> calcPropertyChanges(String namespace, Properties previous, Properties current) {
        if (previous == null) {
            previous = new Properties();
        }

        if (current == null) {
            current = new Properties();
        }

        Set<String> previousKeys = previous.stringPropertyNames();
        Set<String> currentKeys = current.stringPropertyNames();

        Set<String> commonKeys = Sets.intersection(previousKeys, currentKeys); // 交集
        Set<String> newKeys = Sets.difference(currentKeys, commonKeys); // 新集合 - 交集 = 新增
        Set<String> removedKeys = Sets.difference(previousKeys, commonKeys); // 老集合 - 交集 = 移除

        List<ConfigChange> changes = Lists.newArrayList();
        // 计算新增的
        for (String newKey : newKeys) {
            changes.add(new ConfigChange(namespace, newKey, null, current.getProperty(newKey), PropertyChangeType.ADDED));
        }
        // 计算移除的
        for (String removedKey : removedKeys) {
            changes.add(new ConfigChange(namespace, removedKey, previous.getProperty(removedKey), null, PropertyChangeType.DELETED));
        }
        // 计算修改的
        for (String commonKey : commonKeys) {
            String previousValue = previous.getProperty(commonKey);
            String currentValue = current.getProperty(commonKey);
            if (Objects.equal(previousValue, currentValue)) {
                continue;
            }
            changes.add(new ConfigChange(namespace, commonKey, previousValue, currentValue, PropertyChangeType.MODIFIED));
        }

        return changes;
    }

}
