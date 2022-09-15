package com.ctrip.framework.apollo.internals;

import com.ctrip.framework.apollo.build.ApolloInjector;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.core.utils.ClassLoaderUtil;
import com.ctrip.framework.apollo.exceptions.ApolloConfigException;
import com.ctrip.framework.apollo.tracer.Tracer;
import com.ctrip.framework.apollo.tracer.spi.Transaction;
import com.ctrip.framework.apollo.util.ConfigUtil;
import com.ctrip.framework.apollo.util.ExceptionUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * 本地文件配置 Repository 实现类
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public class LocalFileConfigRepository extends AbstractConfigRepository implements RepositoryChangeListener {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileConfigRepository.class);

    /**
     * 配置文件目录
     */
    private static final String CONFIG_DIR = "/config-cache";

    /**
     * Namespace 名字
     */
    private final String m_namespace;
    /**
     * 本地缓存配置文件目录
     */
    private File m_baseDir;
    private final ConfigUtil m_configUtil;
    /**
     * 配置文件 Properties
     */
    private volatile Properties m_fileProperties;
    /**
     * 上游的 ConfigRepository 对象。一般情况下，使用 RemoteConfigRepository 对象，读取远程 Config Service 的配置
     */
    private volatile ConfigRepository m_upstream;

    /**
     * Constructor.
     *
     * @param namespace the namespace
     */
    public LocalFileConfigRepository(String namespace) {
        this(namespace, null);
    }

    /**
     *
     * @param namespace
     * @param upstream 传入的 upstream 可能是 RemoteConfigRepository
     */
    public LocalFileConfigRepository(String namespace, ConfigRepository upstream) {
        m_namespace = namespace;
        m_configUtil = ApolloInjector.getInstance(ConfigUtil.class);
        // 获得本地缓存配置文件的目录
        this.setLocalCacheDir(findLocalCacheDir(), false);
        // 设置 `m_upstream` 属性，并从远端拉了一次配置，更新到properties变量，并且持久化到文件中
        this.setUpstreamRepository(upstream);
        // 同步配置
        this.trySync();
    }

    void setLocalCacheDir(File baseDir, boolean syncImmediately) {
        m_baseDir = baseDir;
        // 获得本地缓存配置文件的目录
        this.checkLocalConfigCacheDir(m_baseDir);
        // 若需要立即同步，则进行同步
        if (syncImmediately) {
            this.trySync();
        }
    }

    /**
     * @return 获得本地缓存文件的目录
     */
    private File findLocalCacheDir() {
        try {
            // 获得默认缓存配置目录
            String defaultCacheDir = m_configUtil.getDefaultLocalCacheDir();
            // 若不存在该目录，进行创建
            Path path = Paths.get(defaultCacheDir);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            // 返回该目录下的 CONFIG_DIR 目录
            if (Files.exists(path) && Files.isWritable(path)) {
                return new File(defaultCacheDir, CONFIG_DIR);
            }
        } catch (Throwable ex) {
            //ignore
        }
        // 若失败，使用 ClassPath 下的 CONFIG_DIR 目录
        return new File(ClassLoaderUtil.getClassPath(), CONFIG_DIR);
    }

    @Override
    public Properties getConfig() {
        // 1、如果 `m_fileProperties` 为空，强制同步（要么从远端拉，要么读取本地文件）
        if (m_fileProperties == null) {
            sync();
        }
        // 2、返回新创建的 `m_fileProperties` 对象，避免原有对象被修改。
        Properties result = new Properties();
        result.putAll(m_fileProperties);
        return result;
    }

    @Override
    public void setUpstreamRepository(ConfigRepository upstreamConfigRepository) {
        if (upstreamConfigRepository == null) {
            return;
        }
        // 从老的 `m_upstream` 移除自己
        // clear previous listener
        if (m_upstream != null) {
            m_upstream.removeChangeListener(this);
        }
        // 设置新的 `m_upstream`
        m_upstream = upstreamConfigRepository;
        // 从 `m_upstream` 拉取配置
        trySyncFromUpstream();
        // 向新的 `m_upstream` 注册自己
        upstreamConfigRepository.addChangeListener(this);
    }

    @Override
    public void onRepositoryChange(String namespace, Properties newProperties) {
        // 忽略，若未变更
        if (newProperties.equals(m_fileProperties)) {
            return;
        }
        // 读取新的 Properties 对象
        Properties newFileProperties = new Properties();
        newFileProperties.putAll(newProperties);
        // 更新到 `m_fileProperties` 中
        updateFileProperties(newFileProperties);
        // 发布 Repository 的配置发生变化，触发对应的监听器们
        super.fireRepositoryChange(namespace, newProperties);
    }

    /**
     * 核心宗旨：同步本地的properties变量
     * 1、从 `m_upstream` 同步配置，并同步到本地
     * 2、若失败，读取本地缓存的配置文件
     */
    @Override
    protected void sync() {
        // do 1、从 `m_upstream` 同步配置，并同步到本地
        // sync with upstream immediately
        boolean syncFromUpstreamResultSuccess = trySyncFromUpstream();
        // 若成功，则直接返回
        if (syncFromUpstreamResultSuccess) {
            return;
        }
        // do 2、若失败，读取本地缓存的配置文件
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "syncLocalConfig");
        Throwable exception = null;
        try {
            transaction.addData("Basedir", m_baseDir.getAbsolutePath());
            // 加载本地缓存的配置文件
            m_fileProperties = this.loadFromLocalCacheFile(m_baseDir, m_namespace);
            transaction.setStatus(Transaction.SUCCESS);
        } catch (Throwable ex) {
            Tracer.logEvent("ApolloConfigException", ExceptionUtil.getDetailMessage(ex));
            transaction.setStatus(ex);
            exception = ex;
            //ignore
        } finally {
            transaction.complete();
        }
        // 若未读取到缓存的配置文件，抛出异常
        if (m_fileProperties == null) {
            throw new ApolloConfigException("Load config from local config failed!", exception);
        }
    }

    /**
     * 1、从 `m_upstream`即server端 拉取配置 Properties
     * 2、更新properties变量并持久化到本地
     */
    private boolean trySyncFromUpstream() {
        if (m_upstream == null) {
            return false;
        }
        try {
            // 从 `m_upstream`即server端 拉取配置 Properties
            Properties properties = m_upstream.getConfig();
            // 更新到 `m_fileProperties` 中，并持久化到本地
            updateFileProperties(properties);
            // 返回同步成功
            return true;
        } catch (Throwable ex) {
            Tracer.logError(ex);
            logger.warn("Sync config from upstream repository {} failed, reason: {}", m_upstream.getClass(), ExceptionUtil.getDetailMessage(ex));
        }
        // 返回同步失败
        return false;
    }

    private synchronized void updateFileProperties(Properties newProperties) {
        // 忽略，若未变更
        if (newProperties.equals(m_fileProperties)) {
            return;
        }
        // 设置新的 Properties 到 `m_fileProperties` 中。
        this.m_fileProperties = newProperties;
        // 持久化到本地缓存配置文件
        persistLocalCacheFile(m_baseDir, m_namespace);
    }

    private Properties loadFromLocalCacheFile(File baseDir, String namespace) {
        Preconditions.checkNotNull(baseDir, "Basedir cannot be null");
        // 拼接本地缓存的配置文件 File 对象
        File file = assembleLocalCacheFile(baseDir, namespace);
        // 从文件中，读取 Properties
        Properties properties;
        if (file.isFile() && file.canRead()) {
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                properties = new Properties();
                properties.load(in); // 读取
                logger.debug("Loading local config file {} successfully!", file.getAbsolutePath());
            } catch (IOException ex) {
                Tracer.logError(ex);
                throw new ApolloConfigException(String.format("Loading config from local cache file %s failed", file.getAbsolutePath()), ex);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                } catch (IOException ex) {
                    // ignore
                }
            }
        } else {
            throw new ApolloConfigException(String.format("Cannot read from local cache file %s", file.getAbsolutePath()));
        }
        return properties;
    }

    void persistLocalCacheFile(File baseDir, String namespace) {
        if (baseDir == null) {
            return;
        }
        // 拼接本地缓存的配置文件 File 对象
        File file = assembleLocalCacheFile(baseDir, namespace);
        // 向文件中，写入 Properties
        OutputStream out = null;
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "persistLocalConfigFile");
        transaction.addData("LocalConfigFile", file.getAbsolutePath());
        try {
            out = new FileOutputStream(file);
            m_fileProperties.store(out, "Persisted by DefaultConfig"); // 写入
            transaction.setStatus(Transaction.SUCCESS);
        } catch (IOException ex) {
            ApolloConfigException exception = new ApolloConfigException(String.format("Persist local cache file %s failed", file.getAbsolutePath()), ex);
            Tracer.logError(exception);
            transaction.setStatus(exception);
            logger.warn("Persist local cache file {} failed, reason: {}.", file.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    //ignore
                }
            }
            transaction.complete();
        }
    }

    /**
     * 校验本地缓存配置目录是否存在。若不存在，则进行创建。
     *
     * @param baseDir 本地缓存目录
     */
    private void checkLocalConfigCacheDir(File baseDir) {
        // 若本地缓存配置文件的目录已经存在，则返回
        if (baseDir.exists()) {
            return;
        }
        Transaction transaction = Tracer.newTransaction("Apollo.ConfigService", "createLocalConfigDir");
        transaction.addData("BaseDir", baseDir.getAbsolutePath());
        try {
            // 创建本地缓存配置目录
            Files.createDirectory(baseDir.toPath());
            transaction.setStatus(Transaction.SUCCESS);
        } catch (IOException ex) {
            ApolloConfigException exception = new ApolloConfigException(String.format("Create local config directory %s failed", baseDir.getAbsolutePath()), ex);
            Tracer.logError(exception);
            transaction.setStatus(exception);
            logger.warn("Unable to create local config cache directory {}, reason: {}. Will not able to cache config file.", baseDir.getAbsolutePath(), ExceptionUtil.getDetailMessage(ex));
        } finally {
            transaction.complete();
        }
    }

    // 拼接完整的本地缓存配置文件的地址
    File assembleLocalCacheFile(File baseDir, String namespace) {
        String fileName = String.format("%s.properties", Joiner.on(ConfigConsts.CLUSTER_NAMESPACE_SEPARATOR) // + 号分隔
                .join(m_configUtil.getAppId(), m_configUtil.getCluster(), namespace));
        return new File(baseDir, fileName);
    }

}