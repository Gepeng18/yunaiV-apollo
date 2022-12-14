package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.*;
import com.ctrip.framework.apollo.biz.repository.ReleaseRepository;
import com.ctrip.framework.apollo.biz.utils.ReleaseKeyGenerator;
import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.constants.ReleaseOperation;
import com.ctrip.framework.apollo.common.constants.ReleaseOperationContext;
import com.ctrip.framework.apollo.common.dto.ItemChangeSets;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.GrayReleaseRuleItemTransformer;
import com.ctrip.framework.apollo.core.utils.StringUtils;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.apache.commons.lang.time.FastDateFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @author Jason Song(song_s@ctrip.com)
 */
@Service
public class ReleaseService {

    private static final FastDateFormat TIMESTAMP_FORMAT = FastDateFormat.getInstance("yyyyMMddHHmmss");
    private Gson gson = new Gson();

    @Autowired
    private ReleaseRepository releaseRepository;
    @Autowired
    private ItemService itemService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private NamespaceLockService namespaceLockService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private NamespaceBranchService namespaceBranchService;
    @Autowired
    private ReleaseHistoryService releaseHistoryService;
    @Autowired
    private ItemSetService itemSetService;

    public Release findOne(long releaseId) {
        return releaseRepository.findOne(releaseId);
    }

    public Release findActiveOne(long releaseId) {
        return releaseRepository.findByIdAndIsAbandonedFalse(releaseId);
    }

    public List<Release> findByReleaseIds(Set<Long> releaseIds) {
        Iterable<Release> releases = releaseRepository.findAll(releaseIds);
        if (releases == null) {
            return Collections.emptyList();
        }
        return Lists.newArrayList(releases);
    }

    public List<Release> findByReleaseKeys(Set<String> releaseKeys) {
        return releaseRepository.findByReleaseKeyIn(releaseKeys);
    }

    public Release findLatestActiveRelease(Namespace namespace) {
        return findLatestActiveRelease(namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName());
    }

    public Release findLatestActiveRelease(String appId, String clusterName, String namespaceName) {
        return releaseRepository.findFirstByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(appId,
                clusterName, namespaceName); // IsAbandoned = False && Id DESC
    }

    public List<Release> findAllReleases(String appId, String clusterName, String namespaceName, Pageable page) {
        List<Release> releases = releaseRepository.findByAppIdAndClusterNameAndNamespaceNameOrderByIdDesc(appId,
                clusterName,
                namespaceName,
                page);
        if (releases == null) {
            return Collections.emptyList();
        }
        return releases;
    }

    public List<Release> findActiveReleases(String appId, String clusterName, String namespaceName, Pageable page) {
        List<Release> releases = releaseRepository.findByAppIdAndClusterNameAndNamespaceNameAndIsAbandonedFalseOrderByIdDesc(
                appId, clusterName, namespaceName, page);
        if (releases == null) {
            return Collections.emptyList();
        }
        return releases;
    }

    // ????????? Namespace ??????????????? Map ?????? Namespace ?????????????????? Release
    @Transactional
    public Release mergeBranchChangeSetsAndRelease(Namespace namespace, String branchName, String releaseName,
                                                   String releaseComment, boolean isEmergencyPublish,
                                                   ItemChangeSets changeSets) {
        // ????????????
        checkLock(namespace, isEmergencyPublish, changeSets.getDataChangeLastModifiedBy());
        // ?????????????????? ??? ItemChangeSets ????????????????????? Namespace ??????
        itemSetService.updateSet(namespace, changeSets);

        // ????????? Namespace ????????????????????? Release ??????
        Release branchRelease = findLatestActiveRelease(namespace.getAppId(), branchName, namespace.getNamespaceName());
        // ????????? Namespace ????????????????????? Release ??????
        long branchReleaseId = branchRelease == null ? 0 : branchRelease.getId();

        // ????????? Namespace ????????? Map
        Map<String, String> operateNamespaceItems = getNamespaceItems(namespace);

        // ?????? Map ????????? ReleaseHistory ????????? `operationContext` ?????????
        Map<String, Object> operationContext = Maps.newHashMap();
        operationContext.put(ReleaseOperationContext.SOURCE_BRANCH, branchName);
        operationContext.put(ReleaseOperationContext.BASE_RELEASE_ID, branchReleaseId);
        operationContext.put(ReleaseOperationContext.IS_EMERGENCY_PUBLISH, isEmergencyPublish);

        // ??? Namespace ????????????
        return masterRelease(namespace, releaseName, releaseComment, operateNamespaceItems,
                changeSets.getDataChangeLastModifiedBy(),
                ReleaseOperation.GRAY_RELEASE_MERGE_TO_MASTER, operationContext);
    }

    // ????????????
    @Transactional
    public Release publish(Namespace namespace, String releaseName, String releaseComment, String operator, boolean isEmergencyPublish) {
        // ????????????
        checkLock(namespace, isEmergencyPublish, operator);
        // ?????? Namespace ??????????????? Map
        Map<String, String> operateNamespaceItems = getNamespaceItems(namespace);
        // ????????? Namespace
        Namespace parentNamespace = namespaceService.findParentNamespace(namespace);
        // ????????? Namespace ???????????? Namespace ?????????????????????
        // branch release
        if (parentNamespace != null) {
            return publishBranchNamespace(parentNamespace, namespace, operateNamespaceItems, releaseName, releaseComment, operator, isEmergencyPublish);
        }
        // ????????? Namespace
        Namespace childNamespace = namespaceService.findChildNamespace(namespace);
        // ????????????????????????????????? Release ??????
        Release previousRelease = null;
        if (childNamespace != null) {
            previousRelease = findLatestActiveRelease(namespace);
        }
        // ???????????? Context
        // master release
        Map<String, Object> operationContext = Maps.newHashMap();
        operationContext.put(ReleaseOperationContext.IS_EMERGENCY_PUBLISH, isEmergencyPublish); // ?????????????????????
        // ????????????
        Release release = masterRelease(namespace, releaseName, releaseComment, operateNamespaceItems, operator, ReleaseOperation.NORMAL_RELEASE, operationContext);
        // ????????? Namespace ????????????????????????????????? Namespace ????????????????????? Namespace ?????????
        // merge to branch and auto release
        if (childNamespace != null) {
            mergeFromMasterAndPublishBranch(namespace, childNamespace, operateNamespaceItems,
                    releaseName, releaseComment, operator, previousRelease,
                    release, isEmergencyPublish);
        }
        return release;
    }

    private void checkLock(Namespace namespace, boolean isEmergencyPublish, String operator) {
        if (!isEmergencyPublish) { // ???????????????
            // ?????? NamespaceLock ??????
            NamespaceLock lock = namespaceLockService.findLock(namespace.getId());
            // ????????????????????????????????????????????????????????? BadRequestException ??????
            if (lock != null && lock.getDataChangeCreatedBy().equals(operator)) {
                throw new BadRequestException("Config can not be published by yourself.");
            }
        }
    }

    // ??????????????????????????? Namespace ????????????????????? Namespace ?????????
    private void mergeFromMasterAndPublishBranch(Namespace parentNamespace, Namespace childNamespace,
                                                 Map<String, String> parentNamespaceItems,
                                                 String releaseName, String releaseComment,
                                                 String operator, Release masterPreviousRelease,
                                                 Release parentRelease, boolean isEmergencyPublish) {
        // ????????? Namespace ????????? Map
        // create release for child namespace
        Map<String, String> childReleaseConfiguration = getNamespaceReleaseConfiguration(childNamespace);
        // ????????? Namespace ????????? Map
        Map<String, String> parentNamespaceOldConfiguration = masterPreviousRelease == null ? null : gson.fromJson(masterPreviousRelease.getConfigurations(), GsonType.CONFIG);

        // ????????????????????? Namespace ????????? Map ????????? Namespace ????????? Map
        Map<String, String> childNamespaceToPublishConfigs = calculateChildNamespaceToPublishConfiguration(parentNamespaceOldConfiguration,
                        parentNamespaceItems, childNamespace);

        // compare
        // ??????????????????????????????????????? Namespace ?????????
        if (!childNamespaceToPublishConfigs.equals(childReleaseConfiguration)) {
            branchRelease(parentNamespace, childNamespace, releaseName, releaseComment,
                    childNamespaceToPublishConfigs, parentRelease.getId(), operator,
                    ReleaseOperation.MASTER_NORMAL_RELEASE_MERGE_TO_GRAY, isEmergencyPublish);
        }
    }

    // ??? Namespace ?????? Release ???
    private Release publishBranchNamespace(Namespace parentNamespace, Namespace childNamespace,
                                           Map<String, String> childNamespaceItems,
                                           String releaseName, String releaseComment,
                                           String operator, boolean isEmergencyPublish) {
        // ????????? Namespace ??????????????? Release ??????
        Release parentLatestRelease = findLatestActiveRelease(parentNamespace);
        // ????????? Namespace ????????????
        Map<String, String> parentConfigurations = parentLatestRelease != null ? gson.fromJson(parentLatestRelease.getConfigurations(), GsonType.CONFIG) : new HashMap<>();
        // ????????? Namespace ??? releaseId ??????
        long baseReleaseId = parentLatestRelease == null ? 0 : parentLatestRelease.getId();
        // ???????????????
        Map<String, String> childNamespaceToPublishConfigs = mergeConfiguration(parentConfigurations, childNamespaceItems);
        // ????????? Namespace ??? Release
        return branchRelease(parentNamespace, childNamespace, releaseName, releaseComment,
                childNamespaceToPublishConfigs, baseReleaseId, operator,
                ReleaseOperation.GRAY_RELEASE, isEmergencyPublish);

    }

    // ??????????????????
    private Release masterRelease(Namespace namespace, String releaseName, String releaseComment,
                                  Map<String, String> configurations, String operator,
                                  int releaseOperation, Map<String, Object> operationContext) {
        // ????????????????????? Release ??????
        Release lastActiveRelease = findLatestActiveRelease(namespace);
        long previousReleaseId = lastActiveRelease == null ? 0 : lastActiveRelease.getId();
        // ?????? Release ??????????????????
        Release release = createRelease(namespace, releaseName, releaseComment, configurations, operator);

        // ?????? ReleaseHistory ??????????????????
        releaseHistoryService.createReleaseHistory(namespace.getAppId(), namespace.getClusterName(),
                namespace.getNamespaceName(), namespace.getClusterName(),
                release.getId(), previousReleaseId, releaseOperation,
                operationContext, operator);
        return release;
    }

    // ????????? Namespace ??? Release
    private Release branchRelease(Namespace parentNamespace, Namespace childNamespace,
                                  String releaseName, String releaseComment,
                                  Map<String, String> configurations, long baseReleaseId,
                                  String operator, int releaseOperation, boolean isEmergencyPublish) {
        // ????????? Namespace ??????????????? Release ??????
        Release previousRelease = findLatestActiveRelease(childNamespace.getAppId(), childNamespace.getClusterName(), childNamespace.getNamespaceName());
        // ????????? Namespace ??????????????? Release ???????????????
        long previousReleaseId = previousRelease == null ? 0 : previousRelease.getId();

        // ?????? Map ????????? ReleaseHistory ????????? `operationContext` ?????????
        Map<String, Object> releaseOperationContext = Maps.newHashMap();
        releaseOperationContext.put(ReleaseOperationContext.BASE_RELEASE_ID, baseReleaseId);
        releaseOperationContext.put(ReleaseOperationContext.IS_EMERGENCY_PUBLISH, isEmergencyPublish);

        // ????????? Namespace ??? Release ??????????????????
        Release release = createRelease(childNamespace, releaseName, releaseComment, configurations, operator);

        // ?????? GrayReleaseRule ??? releaseId ??????
        // update gray release rules
        GrayReleaseRule grayReleaseRule = namespaceBranchService.updateRulesReleaseId(childNamespace.getAppId(),
                parentNamespace.getClusterName(),
                childNamespace.getNamespaceName(),
                childNamespace.getClusterName(),
                release.getId(), operator);

        // ?????? ReleaseHistory ??????????????????
        if (grayReleaseRule != null) {
            releaseOperationContext.put(ReleaseOperationContext.RULES, GrayReleaseRuleItemTransformer.batchTransformFromJSON(grayReleaseRule.getRules()));
        }
        releaseHistoryService.createReleaseHistory(parentNamespace.getAppId(), parentNamespace.getClusterName(),
                parentNamespace.getNamespaceName(), childNamespace.getClusterName(),
                release.getId(),
                previousReleaseId, releaseOperation, releaseOperationContext, operator);
        return release;
    }

    // ???????????????
    private Map<String, String> mergeConfiguration(Map<String, String> baseConfigurations, Map<String, String> coverConfigurations) {
        Map<String, String> result = new HashMap<>();
        // copy base configuration
        // ??? Namespace ????????????
        for (Map.Entry<String, String> entry : baseConfigurations.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        // update and publish
        // ??? Namespace ????????????
        for (Map.Entry<String, String> entry : coverConfigurations.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        // ???????????????????????????
        return result;
    }

    /**
     * ?????? Namespace ??????????????? Map
     *
     * @param namespace Namespace
     * @return ???????????? Map
     */
    private Map<String, String> getNamespaceItems(Namespace namespace) {
        // ?????? Namespace ??? Item ??????
        List<Item> items = itemService.findItemsWithoutOrdered(namespace.getId());
        // ?????????????????? Map ???????????????????????????????????????
        Map<String, String> configurations = new HashMap<String, String>();
        for (Item item : items) {
            if (StringUtils.isEmpty(item.getKey())) {
                continue;
            }
            configurations.put(item.getKey(), item.getValue());
        }
        return configurations;
    }

    // ?????? Namespace ????????? Map
    private Map<String, String> getNamespaceReleaseConfiguration(Namespace namespace) {
        // ????????????????????? Release ??????
        Release release = findLatestActiveRelease(namespace);
        Map<String, String> configuration = new HashMap<>();
        // ???????????? Map
        if (release != null) {
            configuration = new Gson().fromJson(release.getConfigurations(), GsonType.CONFIG);
        }
        return configuration;
    }

    // ?????? Release ??????????????????
    private Release createRelease(Namespace namespace, String name, String comment,
                                  Map<String, String> configurations, String operator) {
        // ?????? Release ??????
        Release release = new Release();
        release.setReleaseKey(ReleaseKeyGenerator.generateReleaseKey(namespace)); //???TODO 6006???Release Key ?????????
        release.setDataChangeCreatedTime(new Date());
        release.setDataChangeCreatedBy(operator);
        release.setDataChangeLastModifiedBy(operator);
        release.setName(name);
        release.setComment(comment);
        release.setAppId(namespace.getAppId());
        release.setClusterName(namespace.getClusterName());
        release.setNamespaceName(namespace.getNamespaceName());
        release.setConfigurations(gson.toJson(configurations)); // ?????? Gson ???????????? Map ????????????????????????
        // ?????? Release ??????
        release = releaseRepository.save(release);
        // ?????? NamespaceLock
        namespaceLockService.unlock(namespace.getId());
        // ?????? Audit ???????????????
        auditService.audit(Release.class.getSimpleName(), release.getId(), Audit.OP.INSERT, release.getDataChangeCreatedBy());
        return release;
    }

    @Transactional
    public Release rollback(long releaseId, String operator) {
        Release release = findOne(releaseId);
        if (release == null) {
            throw new NotFoundException("release not found");
        }
        if (release.isAbandoned()) {
            throw new BadRequestException("release is not active");
        }

        String appId = release.getAppId();
        String clusterName = release.getClusterName();
        String namespaceName = release.getNamespaceName();

        PageRequest page = new PageRequest(0, 2);
        List<Release> twoLatestActiveReleases = findActiveReleases(appId, clusterName, namespaceName, page);
        if (twoLatestActiveReleases == null || twoLatestActiveReleases.size() < 2) {
            throw new BadRequestException(String.format(
                    "Can't rollback namespace(appId=%s, clusterName=%s, namespaceName=%s) because there is only one active release",
                    appId,
                    clusterName,
                    namespaceName));
        }

        release.setAbandoned(true);
        release.setDataChangeLastModifiedBy(operator);

        releaseRepository.save(release);

        releaseHistoryService.createReleaseHistory(appId, clusterName,
                namespaceName, clusterName, twoLatestActiveReleases.get(1).getId(),
                release.getId(), ReleaseOperation.ROLLBACK, null, operator);

        //publish child namespace if namespace has child
        rollbackChildNamespace(appId, clusterName, namespaceName, twoLatestActiveReleases, operator);

        return release;
    }

    private void rollbackChildNamespace(String appId, String clusterName, String namespaceName,
                                        List<Release> parentNamespaceTwoLatestActiveRelease, String operator) {
        Namespace parentNamespace = namespaceService.findOne(appId, clusterName, namespaceName);
        Namespace childNamespace = namespaceService.findChildNamespace(appId, clusterName, namespaceName);
        if (parentNamespace == null || childNamespace == null) {
            return;
        }

        Release abandonedRelease = parentNamespaceTwoLatestActiveRelease.get(0);
        Release parentNamespaceNewLatestRelease = parentNamespaceTwoLatestActiveRelease.get(1);

        Map<String, String> parentNamespaceAbandonedConfiguration = gson.fromJson(abandonedRelease.getConfigurations(),
                GsonType.CONFIG);

        Map<String, String>
                parentNamespaceNewLatestConfiguration =
                gson.fromJson(parentNamespaceNewLatestRelease.getConfigurations(), GsonType.CONFIG);

        Map<String, String>
                childNamespaceNewConfiguration =
                calculateChildNamespaceToPublishConfiguration(parentNamespaceAbandonedConfiguration,
                        parentNamespaceNewLatestConfiguration,
                        childNamespace);

        branchRelease(parentNamespace, childNamespace,
                TIMESTAMP_FORMAT.format(new Date()) + "-master-rollback-merge-to-gray", "",
                childNamespaceNewConfiguration, parentNamespaceNewLatestRelease.getId(), operator,
                ReleaseOperation.MATER_ROLLBACK_MERGE_TO_GRAY, false);
    }

    // ????????????????????? Namespace ????????? Map ????????? Namespace ????????? Map
    private Map<String, String> calculateChildNamespaceToPublishConfiguration(
            Map<String, String> parentNamespaceOldConfiguration, Map<String, String> parentNamespaceNewConfiguration,
            Namespace childNamespace) {
        // ????????? Namespace ?????????????????? Release ??????
        // first. calculate child namespace modified configs
        Release childNamespaceLatestActiveRelease = findLatestActiveRelease(childNamespace);
        // ????????? Namespace ????????? Map
        Map<String, String> childNamespaceLatestActiveConfiguration = childNamespaceLatestActiveRelease == null ? null :
                gson.fromJson(childNamespaceLatestActiveRelease.getConfigurations(), GsonType.CONFIG);

        // ?????? Namespace ????????? Map ?????????????????????????????? Map
        Map<String, String> childNamespaceModifiedConfiguration = calculateBranchModifiedItemsAccordingToRelease(parentNamespaceOldConfiguration,
                childNamespaceLatestActiveConfiguration);

        // second. append child namespace modified configs to parent namespace new latest configuration
        return mergeConfiguration(parentNamespaceNewConfiguration, childNamespaceModifiedConfiguration);
    }

    // ?????? Namespace ????????? Map ?????????????????????????????? Map
    private Map<String, String> calculateBranchModifiedItemsAccordingToRelease(
            Map<String, String> masterReleaseConfigs,
            Map<String, String> branchReleaseConfigs) {
        // ?????? Map
        Map<String, String> modifiedConfigs = new HashMap<>();
        // ?????? Namespace ????????? Map ???????????????????????? Map
        if (CollectionUtils.isEmpty(branchReleaseConfigs)) {
            return modifiedConfigs;
        }
        // ?????? Namespace ????????? Map ???????????????????????? Namespace ????????? Map
        if (CollectionUtils.isEmpty(masterReleaseConfigs)) {
            return branchReleaseConfigs;
        }

        // ?????? Namespace ????????? Map ?????????????????????????????? Map
        for (Map.Entry<String, String> entry : branchReleaseConfigs.entrySet()) {
            if (!Objects.equals(entry.getValue(), masterReleaseConfigs.get(entry.getKey()))) { // ??????
                modifiedConfigs.put(entry.getKey(), entry.getValue());
            }
        }
        return modifiedConfigs;
    }

    @Transactional
    public int batchDelete(String appId, String clusterName, String namespaceName, String operator) {
        return releaseRepository.batchDelete(appId, clusterName, namespaceName, operator);
    }

}
