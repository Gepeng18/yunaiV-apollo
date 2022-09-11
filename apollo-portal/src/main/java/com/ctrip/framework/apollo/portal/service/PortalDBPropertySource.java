package com.ctrip.framework.apollo.portal.service;

import com.ctrip.framework.apollo.common.config.RefreshablePropertySource;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 基于 PortalDB 的 ServerConfig 的 PropertySource 实现类。
 *
 * @author Jason Song(song_s@ctrip.com)
 */
@Component
public class PortalDBPropertySource extends RefreshablePropertySource {

    private static final Logger logger = LoggerFactory.getLogger(PortalDBPropertySource.class);

    @Autowired
    private ServerConfigRepository serverConfigRepository;

    public PortalDBPropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    public PortalDBPropertySource() {
        super("DBConfig", Maps.newConcurrentMap());
    }

    @Override
    protected void refresh() {
        // 获得所有的 ServerConfig 记录
        Iterable<ServerConfig> dbConfigs = serverConfigRepository.findAll();
        // 缓存，更新到属性源
        for (ServerConfig config : dbConfigs) {
            String key = config.getKey();
            Object value = config.getValue();

            // 打印日志
            if (this.source.isEmpty()) {
                logger.info("Load config from DB : {} = {}", key, value);
            } else if (!Objects.equals(this.source.get(key), value)) {
                logger.info("Load config from DB : {} = {}. Old value = {}", key, value, this.source.get(key));
            }

            // 更新到属性源
            this.source.put(key, value);
        }
    }

}
