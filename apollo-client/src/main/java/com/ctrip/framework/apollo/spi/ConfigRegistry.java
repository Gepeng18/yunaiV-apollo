package com.ctrip.framework.apollo.spi;

/**
 * The manually config registry, use with caution!
 *
 * Config 注册表接口。其中，KEY 为 Namespace 的名字，VALUE 为 ConfigFactory 对象
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigRegistry {

    /**
     * Register the config factory for the namespace specified.
     *
     * @param namespace the namespace
     * @param factory   the factory for this namespace
     */
    void register(String namespace, ConfigFactory factory);

    /**
     * Get the registered config factory for the namespace.
     *
     * @param namespace the namespace
     * @return the factory registered for this namespace
     */
    ConfigFactory getFactory(String namespace);

}