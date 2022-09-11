package com.ctrip.framework.apollo.portal.controller;


import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.portal.entity.po.ServerConfig;
import com.ctrip.framework.apollo.portal.repository.ServerConfigRepository;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

import static com.ctrip.framework.apollo.common.utils.RequestPrecondition.checkModel;

/**
 * ServerConfig Controller
 *
 * 配置中心本身需要一些配置,这些配置放在数据库里面
 */
@RestController
public class ServerConfigController {

    @Autowired
    private ServerConfigRepository serverConfigRepository;
    @Autowired
    private UserInfoHolder userInfoHolder;

    @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
    @RequestMapping(value = "/server/config", method = RequestMethod.POST)
    public ServerConfig createOrUpdate(@RequestBody ServerConfig serverConfig) {
        // 校验 ServerConfig 非空
        checkModel(Objects.nonNull(serverConfig));
        // 校验 ServerConfig 的 `key` `value` 属性非空
        RequestPrecondition.checkArgumentsNotEmpty(serverConfig.getKey(), serverConfig.getValue());
        // 获得操作人为当前管理员
        String modifiedBy = userInfoHolder.getUser().getUserId();
        // 查询当前 DB 里的对应的 ServerConfig 对象
        ServerConfig storedConfig = serverConfigRepository.findByKey(serverConfig.getKey());
        // 若不存在，则进行新增
        if (Objects.isNull(storedConfig)) {//create
            serverConfig.setDataChangeCreatedBy(modifiedBy);
            serverConfig.setDataChangeLastModifiedBy(modifiedBy);
            return serverConfigRepository.save(serverConfig);
        // 若存在，则进行更新
        } else { //update
            BeanUtils.copyEntityProperties(serverConfig, storedConfig); // 复制属性，serverConfig => storedConfig
            storedConfig.setDataChangeLastModifiedBy(modifiedBy);
            return serverConfigRepository.save(storedConfig);
        }
    }

}
