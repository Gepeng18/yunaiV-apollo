package com.ctrip.framework.apollo;

import com.ctrip.framework.apollo.model.ConfigChangeEvent;

/**
 * {@link Config} 变化监听器
 *
 * @author Jason Song(song_s@ctrip.com)
 */
public interface ConfigChangeListener {

    /**
     * Invoked when there is any config change for the namespace.
     *
     * @param changeEvent the event for this change
     */
    void onChange(ConfigChangeEvent changeEvent);

}