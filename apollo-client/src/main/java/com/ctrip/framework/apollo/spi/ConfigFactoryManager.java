package com.ctrip.framework.apollo.spi;

/**
 * ConfigFactory 管理器接口
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigFactoryManager {

    /**
     * Get the config factory for the namespace.
     *
     * @param namespace the namespace
     * @return the config factory for this namespace
     */
    ConfigFactory getFactory(String namespace);

}