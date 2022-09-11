package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.NamespaceDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.service.ConsumerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@RestController
public class ConsumerController {

    private static final Date DEFAULT_EXPIRES = new GregorianCalendar(2099, Calendar.JANUARY, 1).getTime();

    @Autowired
    private ConsumerService consumerService;

    // 创建 Consumer
    @Transactional
    @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
    @RequestMapping(value = "/consumers", method = RequestMethod.POST)
    public ConsumerToken createConsumer(@RequestBody Consumer consumer,
                                        @RequestParam(value = "expires", required = false)
                                        @DateTimeFormat(pattern = "yyyyMMddHHmmss") Date
                                                expires) {
        // 校验非空
        if (StringUtils.isContainEmpty(consumer.getAppId(), consumer.getName(), consumer.getOwnerName(), consumer.getOrgId())) {
            throw new BadRequestException("Params(appId、name、ownerName、orgId) can not be empty.");
        }

        // 创建 Consumer 对象，并保存到数据库中
        Consumer createdConsumer = consumerService.createConsumer(consumer);

        // 创建 ConsumerToken 对象，并保存到数据库中
        if (Objects.isNull(expires)) {
            expires = DEFAULT_EXPIRES;
        }
        return consumerService.generateAndSaveConsumerToken(createdConsumer, expires);
    }

    @RequestMapping(value = "/consumers/by-appId", method = RequestMethod.GET)
    public ConsumerToken getConsumerTokenByAppId(@RequestParam String appId) {
        return consumerService.getConsumerTokenByAppId(appId);
    }

    // 授权给 Consumer
    @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
    @RequestMapping(value = "/consumers/{token}/assign-role", method = RequestMethod.POST)
    public List<ConsumerRole> assignNamespaceRoleToConsumer(@PathVariable String token, @RequestParam String type, @RequestBody NamespaceDTO namespace) {
        String appId = namespace.getAppId();
        String namespaceName = namespace.getNamespaceName();
        // 校验 appId 非空。若为空，抛出 BadRequestException 异常
        if (StringUtils.isEmpty(appId)) {
            throw new BadRequestException("Params(AppId) can not be empty.");
        }

        // 授权 App 的 Role 给 Consumer
        if (Objects.equals("AppRole", type)) {
            return Collections.singletonList(consumerService.assignAppRoleToConsumer(token, appId));
        // 授权 Namespace 的 Role 给 Consumer
        } else {
            if (StringUtils.isEmpty(namespaceName)) {
                throw new BadRequestException("Params(NamespaceName) can not be empty.");
            }
            return consumerService.assignNamespaceRoleToConsumer(token, appId, namespaceName);
        }
    }

}
