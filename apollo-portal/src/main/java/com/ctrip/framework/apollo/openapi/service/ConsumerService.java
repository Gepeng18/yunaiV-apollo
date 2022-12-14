package com.ctrip.framework.apollo.openapi.service;

import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.openapi.entity.Consumer;
import com.ctrip.framework.apollo.openapi.entity.ConsumerAudit;
import com.ctrip.framework.apollo.openapi.entity.ConsumerRole;
import com.ctrip.framework.apollo.openapi.entity.ConsumerToken;
import com.ctrip.framework.apollo.openapi.repository.ConsumerAuditRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerRoleRepository;
import com.ctrip.framework.apollo.openapi.repository.ConsumerTokenRepository;
import com.ctrip.framework.apollo.portal.component.config.PortalConfig;
import com.ctrip.framework.apollo.portal.entity.bo.UserInfo;
import com.ctrip.framework.apollo.portal.entity.po.Role;
import com.ctrip.framework.apollo.portal.service.RolePermissionService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import com.ctrip.framework.apollo.portal.spi.UserService;
import com.ctrip.framework.apollo.portal.util.RoleUtils;
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.hash.Hashing;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class ConsumerService {

    private static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");
    private static final Joiner KEY_JOINER = Joiner.on("|");

    @Autowired
    private UserInfoHolder userInfoHolder;
    @Autowired
    private ConsumerTokenRepository consumerTokenRepository;
    @Autowired
    private ConsumerRepository consumerRepository;
    @Autowired
    private ConsumerAuditRepository consumerAuditRepository;
    @Autowired
    private ConsumerRoleRepository consumerRoleRepository;
    @Autowired
    private PortalConfig portalConfig;
    @Autowired
    private RolePermissionService rolePermissionService;
    @Autowired
    private UserService userService;

    public Consumer createConsumer(Consumer consumer) {
        String appId = consumer.getAppId();

        // ?????? appId ????????? Consumer ?????????
        Consumer managedConsumer = consumerRepository.findByAppId(appId);
        if (managedConsumer != null) {
            throw new BadRequestException("Consumer already exist");
        }

        // ?????? ownerName ????????? UserInfo ??????
        String ownerName = consumer.getOwnerName();
        UserInfo owner = userService.findByUserId(ownerName);
        if (owner == null) {
            throw new BadRequestException(String.format("User does not exist. UserId = %s", ownerName));
        }
        consumer.setOwnerEmail(owner.getEmail());

        // ?????? Consumer ?????????????????????????????????????????????
        String operator = userInfoHolder.getUser().getUserId();
        consumer.setDataChangeCreatedBy(operator);
        consumer.setDataChangeLastModifiedBy(operator);

        // ?????? Consumer ???????????????
        return consumerRepository.save(consumer);
    }

    public ConsumerToken generateAndSaveConsumerToken(Consumer consumer, Date expires) {
        Preconditions.checkArgument(consumer != null, "Consumer can not be null");

        // ?????? ConsumerToken ??????
        ConsumerToken consumerToken = generateConsumerToken(consumer, expires);
        consumerToken.setId(0); //for protection

        // ?????? ConsumerToken ???????????????
        return consumerTokenRepository.save(consumerToken);
    }

    public ConsumerToken getConsumerTokenByAppId(String appId) {
        Consumer consumer = consumerRepository.findByAppId(appId);
        if (consumer == null) {
            return null;
        }

        return consumerTokenRepository.findByConsumerId(consumer.getId());
    }

    public Long getConsumerIdByToken(String token) {
        if (Strings.isNullOrEmpty(token)) {
            return null;
        }
        ConsumerToken consumerToken = consumerTokenRepository.findTopByTokenAndExpiresAfter(token, new Date());
        return consumerToken == null ? null : consumerToken.getConsumerId();
    }

    public Consumer getConsumerByConsumerId(long consumerId) {
        return consumerRepository.findOne(consumerId);
    }

    // ?????? Namespace ??? Role ??? Consumer
    @Transactional
    public List<ConsumerRole> assignNamespaceRoleToConsumer(String token, String appId, String namespaceName) {
        // ?????? Token ?????????????????? Consumer ???????????????????????? BadRequestException ??????
        Long consumerId = getConsumerIdByToken(token);
        if (consumerId == null) {
            throw new BadRequestException("Token is Illegal");
        }

        // ?????? Namespace ????????? Role ???????????????????????????????????? BadRequestException ??????
        Role namespaceModifyRole = rolePermissionService.findRoleByRoleName(RoleUtils.buildModifyNamespaceRoleName(appId, namespaceName));
        Role namespaceReleaseRole = rolePermissionService.findRoleByRoleName(RoleUtils.buildReleaseNamespaceRoleName(appId, namespaceName));
        if (namespaceModifyRole == null || namespaceReleaseRole == null) {
            throw new BadRequestException("Namespace's role does not exist. Please check whether namespace has created.");
        }
        long namespaceModifyRoleId = namespaceModifyRole.getId();
        long namespaceReleaseRoleId = namespaceReleaseRole.getId();

        // ?????? Consumer ????????? ConsumerRole ??????????????????????????? ConsumerRole ??????
        ConsumerRole managedModifyRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, namespaceModifyRoleId);
        ConsumerRole managedReleaseRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, namespaceReleaseRoleId);
        if (managedModifyRole != null && managedReleaseRole != null) {
            return Arrays.asList(managedModifyRole, managedReleaseRole);
        }

        // ?????? Consumer ????????? ConsumerRole ???
        String operator = userInfoHolder.getUser().getUserId();
        ConsumerRole namespaceModifyConsumerRole = createConsumerRole(consumerId, namespaceModifyRoleId, operator);
        ConsumerRole namespaceReleaseConsumerRole = createConsumerRole(consumerId, namespaceReleaseRoleId, operator);
        // ?????? Consumer ????????? ConsumerRole ??????????????????
        ConsumerRole createdModifyConsumerRole = consumerRoleRepository.save(namespaceModifyConsumerRole);
        ConsumerRole createdReleaseConsumerRole = consumerRoleRepository.save(namespaceReleaseConsumerRole);
        // ?????? ConsumerRole ??????
        return Arrays.asList(createdModifyConsumerRole, createdReleaseConsumerRole);
    }

    // ?????? App ??? Role ??? Consumer
    @Transactional
    public ConsumerRole assignAppRoleToConsumer(String token, String appId) {
        // ?????? Token ?????????????????? Consumer ???????????????????????? BadRequestException ??????
        Long consumerId = getConsumerIdByToken(token);
        if (consumerId == null) {
            throw new BadRequestException("Token is Illegal");
        }

        // ?????? App ????????? Role ??????
        Role masterRole = rolePermissionService.findRoleByRoleName(RoleUtils.buildAppMasterRoleName(appId));
        if (masterRole == null) {
            throw new BadRequestException("App's role does not exist. Please check whether app has created.");
        }

        // ?????? Consumer ????????? ConsumerRole ?????????????????????????????? ConsumerRole ??????
        long roleId = masterRole.getId();
        ConsumerRole managedModifyRole = consumerRoleRepository.findByConsumerIdAndRoleId(consumerId, roleId);
        if (managedModifyRole != null) {
            return managedModifyRole;
        }

        // ?????? Consumer ????????? ConsumerRole ??????
        String operator = userInfoHolder.getUser().getUserId();
        ConsumerRole consumerRole = createConsumerRole(consumerId, roleId, operator);
        // ?????? Consumer ????????? ConsumerRole ??????
        return consumerRoleRepository.save(consumerRole);
    }

    @Transactional
    public void createConsumerAudits(Iterable<ConsumerAudit> consumerAudits) {
        consumerAuditRepository.save(consumerAudits);
    }

    @Transactional // ??????????????????
    public ConsumerToken createConsumerToken(ConsumerToken entity) {
        entity.setId(0); //for protection

        return consumerTokenRepository.save(entity);
    }

    private ConsumerToken generateConsumerToken(Consumer consumer, Date expires) {
        long consumerId = consumer.getId();
        String createdBy = userInfoHolder.getUser().getUserId();
        Date createdTime = new Date();

        // ?????? ConsumerToken
        ConsumerToken consumerToken = new ConsumerToken();
        consumerToken.setConsumerId(consumerId);
        consumerToken.setExpires(expires);
        consumerToken.setDataChangeCreatedBy(createdBy);
        consumerToken.setDataChangeCreatedTime(createdTime);
        consumerToken.setDataChangeLastModifiedBy(createdBy);
        consumerToken.setDataChangeLastModifiedTime(createdTime);

        // ?????? ConsumerToken ??? `token`
        generateAndEnrichToken(consumer, consumerToken);

        return consumerToken;
    }

    void generateAndEnrichToken(Consumer consumer, ConsumerToken consumerToken) {
        Preconditions.checkArgument(consumer != null);

        // ??????????????????
        if (consumerToken.getDataChangeCreatedTime() == null) {
            consumerToken.setDataChangeCreatedTime(new Date());
        }
        // ?????? ConsumerToken ??? `token`
        consumerToken.setToken(generateToken(consumer.getAppId(), consumerToken.getDataChangeCreatedTime(), portalConfig.consumerTokenSalt()));
    }

    /**
     * ?????? {@link ConsumerToken} ??? Token
     *
     * @param consumerAppId     Consumer App ??????
     * @param generationTime    ????????????
     * @param consumerTokenSalt Salt
     * @return Token
     */
    String generateToken(String consumerAppId, Date generationTime, String consumerTokenSalt) {
        return Hashing.sha1().hashString(KEY_JOINER.join(consumerAppId, TIMESTAMP_FORMAT.format(generationTime), consumerTokenSalt), Charsets.UTF_8).toString();
    }

    ConsumerRole createConsumerRole(Long consumerId, Long roleId, String operator) {
        ConsumerRole consumerRole = new ConsumerRole();
        consumerRole.setConsumerId(consumerId);
        consumerRole.setRoleId(roleId);
        consumerRole.setDataChangeCreatedBy(operator);
        consumerRole.setDataChangeLastModifiedBy(operator);
        return consumerRole;
    }

}
