package com.ctrip.framework.apollo.biz.service;

import com.ctrip.framework.apollo.biz.entity.*;
import com.ctrip.framework.apollo.biz.message.MessageSender;
import com.ctrip.framework.apollo.biz.message.Topics;
import com.ctrip.framework.apollo.biz.repository.NamespaceRepository;
import com.ctrip.framework.apollo.biz.utils.ReleaseMessageKeyGenerator;
import com.ctrip.framework.apollo.common.constants.GsonType;
import com.ctrip.framework.apollo.common.constants.NamespaceBranchStatus;
import com.ctrip.framework.apollo.common.entity.AppNamespace;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.ServiceException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Namespace Service
 */
@Service
public class NamespaceService {

    private Gson gson = new Gson();

    @Autowired
    private NamespaceRepository namespaceRepository;
    @Autowired
    private AuditService auditService;
    @Autowired
    private AppNamespaceService appNamespaceService;
    @Autowired
    private ItemService itemService;
    @Autowired
    private CommitService commitService;
    @Autowired
    private ReleaseService releaseService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private NamespaceBranchService namespaceBranchService;
    @Autowired
    private ReleaseHistoryService releaseHistoryService;
    @Autowired
    private NamespaceLockService namespaceLockService;
    @Autowired
    private InstanceService instanceService;
    @Autowired
    private MessageSender messageSender;

    public Namespace findOne(Long namespaceId) {
        return namespaceRepository.findOne(namespaceId);
    }

    public Namespace findOne(String appId, String clusterName, String namespaceName) {
        return namespaceRepository.findByAppIdAndClusterNameAndNamespaceName(appId, clusterName, namespaceName);
    }

    public Namespace findPublicNamespaceForAssociatedNamespace(String clusterName, String namespaceName) {
        AppNamespace appNamespace = appNamespaceService.findPublicNamespaceByName(namespaceName);
        if (appNamespace == null) {
            throw new BadRequestException("namespace not exist");
        }

        String appId = appNamespace.getAppId();

        Namespace namespace = findOne(appId, clusterName, namespaceName);

        //default cluster's namespace
        if (Objects.equals(clusterName, ConfigConsts.CLUSTER_NAME_DEFAULT)) {
            return namespace;
        }

        //custom cluster's namespace not exist.
        //return default cluster's namespace
        if (namespace == null) {
            return findOne(appId, ConfigConsts.CLUSTER_NAME_DEFAULT, namespaceName);
        }

        //custom cluster's namespace exist and has published.
        //return custom cluster's namespace
        Release latestActiveRelease = releaseService.findLatestActiveRelease(namespace);
        if (latestActiveRelease != null) {
            return namespace;
        }

        Namespace defaultNamespace = findOne(appId, ConfigConsts.CLUSTER_NAME_DEFAULT, namespaceName);

        //custom cluster's namespace exist but never published.
        //and default cluster's namespace not exist.
        //return custom cluster's namespace
        if (defaultNamespace == null) {
            return namespace;
        }

        //custom cluster's namespace exist but never published.
        //and default cluster's namespace exist and has published.
        //return default cluster's namespace
        Release defaultNamespaceLatestActiveRelease = releaseService.findLatestActiveRelease(defaultNamespace);
        if (defaultNamespaceLatestActiveRelease != null) {
            return defaultNamespace;
        }

        //custom cluster's namespace exist but never published.
        //and default cluster's namespace exist but never published.
        //return custom cluster's namespace
        return namespace;
    }

    public List<Namespace> findPublicAppNamespaceAllNamespaces(String namespaceName, Pageable page) {
        AppNamespace publicAppNamespace = appNamespaceService.findPublicNamespaceByName(namespaceName);

        if (publicAppNamespace == null) {
            throw new BadRequestException(
                    String.format("Public appNamespace not exists. NamespaceName = %s", namespaceName));
        }

        List<Namespace> namespaces = namespaceRepository.findByNamespaceName(namespaceName, page);

        return filterChildNamespace(namespaces);
    }

    private List<Namespace> filterChildNamespace(List<Namespace> namespaces) {
        List<Namespace> result = new LinkedList<>();

        if (CollectionUtils.isEmpty(namespaces)) {
            return result;
        }

        for (Namespace namespace : namespaces) {
            if (!isChildNamespace(namespace)) {
                result.add(namespace);
            }
        }

        return result;
    }

    public int countPublicAppNamespaceAssociatedNamespaces(String publicNamespaceName) {
        AppNamespace publicAppNamespace = appNamespaceService.findPublicNamespaceByName(publicNamespaceName);

        if (publicAppNamespace == null) {
            throw new BadRequestException(
                    String.format("Public appNamespace not exists. NamespaceName = %s", publicNamespaceName));
        }

        return namespaceRepository.countByNamespaceNameAndAppIdNot(publicNamespaceName, publicAppNamespace.getAppId());
    }

    public List<Namespace> findNamespaces(String appId, String clusterName) {
        List<Namespace> namespaces = namespaceRepository.findByAppIdAndClusterNameOrderByIdAsc(appId, clusterName);
        if (namespaces == null) {
            return Collections.emptyList();
        }
        return namespaces;
    }

    public List<Namespace> findByAppIdAndNamespaceName(String appId, String namespaceName) {
        return namespaceRepository.findByAppIdAndNamespaceName(appId, namespaceName);
    }

    /**
     * ??????????????? Namespace ?????? Namespace ??????
     *
     * @param appId App ??????
     * @param parentClusterName ??? Cluster ?????????
     * @param namespaceName ??? Namespace ?????????
     * @return ??? Namespace ??????
     */
    public Namespace findChildNamespace(String appId, String parentClusterName, String namespaceName) {
        // ?????? Namespace ??????
        List<Namespace> namespaces = findByAppIdAndNamespaceName(appId, namespaceName);
        // ??????????????? Namespace ?????????????????? Namespace
        if (CollectionUtils.isEmpty(namespaces) || namespaces.size() == 1) {
            return null;
        }
        // ?????? Cluster ??????
        List<Cluster> childClusters = clusterService.findChildClusters(appId, parentClusterName);
        // ????????? Cluster ?????????????????? Namespace
        if (CollectionUtils.isEmpty(childClusters)) {
            return null;
        }
        // ????????? Cluster ??????????????????
        Set<String> childClusterNames = childClusters.stream().map(Cluster::getName).collect(Collectors.toSet());
        // ?????? Namespace ??????????????? Cluster ??????????????????????????????????????? Namespace ?????????
        // the child namespace is the intersection of the child clusters and child namespaces
        for (Namespace namespace : namespaces) {
            if (childClusterNames.contains(namespace.getClusterName())) {
                return namespace;
            }
        }
        // ?????? Namespace ???????????????
        return null;
    }

    public Namespace findChildNamespace(Namespace parentNamespace) {
        String appId = parentNamespace.getAppId();
        String parentClusterName = parentNamespace.getClusterName();
        String namespaceName = parentNamespace.getNamespaceName();
        //
        return findChildNamespace(appId, parentClusterName, namespaceName);
    }

    public Namespace findParentNamespace(String appId, String clusterName, String namespaceName) {
        return findParentNamespace(new Namespace(appId, clusterName, namespaceName));
    }

    // ????????? Namespace
    public Namespace findParentNamespace(Namespace namespace) {
        String appId = namespace.getAppId();
        String namespaceName = namespace.getNamespaceName();
        // ?????? Cluster
        Cluster cluster = clusterService.findOne(appId, namespace.getClusterName());
        // ????????? Cluster
        if (cluster != null && cluster.getParentClusterId() > 0) {
            // ????????? Cluster
            Cluster parentCluster = clusterService.findOne(cluster.getParentClusterId());
            // ????????? Namespace
            return findOne(appId, parentCluster.getName(), namespaceName);
        }
        return null;
    }

    public boolean isChildNamespace(String appId, String clusterName, String namespaceName) {
        return isChildNamespace(new Namespace(appId, clusterName, namespaceName));
    }

    public boolean isChildNamespace(Namespace namespace) {
        return findParentNamespace(namespace) != null;
    }

    public boolean isNamespaceUnique(String appId, String cluster, String namespace) {
        Objects.requireNonNull(appId, "AppId must not be null");
        Objects.requireNonNull(cluster, "Cluster must not be null");
        Objects.requireNonNull(namespace, "Namespace must not be null");
        return Objects.isNull(namespaceRepository.findByAppIdAndClusterNameAndNamespaceName(appId, cluster, namespace));
    }

    @Transactional
    public void deleteByAppIdAndClusterName(String appId, String clusterName, String operator) {
        List<Namespace> toDeleteNamespaces = findNamespaces(appId, clusterName);
        for (Namespace namespace : toDeleteNamespaces) {
            deleteNamespace(namespace, operator);
        }
    }

    @Transactional
    public Namespace deleteNamespace(Namespace namespace, String operator) {
        String appId = namespace.getAppId();
        String clusterName = namespace.getClusterName();
        String namespaceName = namespace.getNamespaceName();

        itemService.batchDelete(namespace.getId(), operator);
        commitService.batchDelete(appId, clusterName, namespace.getNamespaceName(), operator);

        if (!isChildNamespace(namespace)) {
            releaseService.batchDelete(appId, clusterName, namespace.getNamespaceName(), operator);
        }

        //delete child namespace
        Namespace childNamespace = findChildNamespace(namespace);
        if (childNamespace != null) {
            namespaceBranchService.deleteBranch(appId, clusterName, namespaceName,
                    childNamespace.getClusterName(), NamespaceBranchStatus.DELETED, operator);
            //delete child namespace's releases. Notice: delete child namespace will not delete child namespace's releases
            releaseService.batchDelete(appId, childNamespace.getClusterName(), namespaceName, operator);
        }

        releaseHistoryService.batchDelete(appId, clusterName, namespaceName, operator);

        instanceService.batchDeleteInstanceConfig(appId, clusterName, namespaceName);

        namespaceLockService.unlock(namespace.getId());

        namespace.setDeleted(true);
        namespace.setDataChangeLastModifiedBy(operator);

        // ?????? Audit ???????????????
        auditService.audit(Namespace.class.getSimpleName(), namespace.getId(), Audit.OP.DELETE, operator);

        Namespace deleted = namespaceRepository.save(namespace);

        //Publish release message to do some clean up in config service, such as updating the cache
        messageSender.sendMessage(ReleaseMessageKeyGenerator.generate(appId, clusterName, namespaceName),
                Topics.APOLLO_RELEASE_TOPIC);

        return deleted;
    }

    @Transactional
    public Namespace save(Namespace entity) {
        // ?????????????????????????????????????????? ServiceException ?????????
        if (!isNamespaceUnique(entity.getAppId(), entity.getClusterName(), entity.getNamespaceName())) {
            throw new ServiceException("namespace not unique");
        }
        // ????????????????????? Namespace ????????????????????? id ?????????
        entity.setId(0);//protection
        // ?????? Namespace ????????????
        Namespace namespace = namespaceRepository.save(entity);
        /// ?????? Audit ???????????????
        auditService.audit(Namespace.class.getSimpleName(), namespace.getId(), Audit.OP.INSERT, namespace.getDataChangeCreatedBy());
        return namespace;
    }

    @Transactional
    public Namespace update(Namespace namespace) {
        Namespace managedNamespace = namespaceRepository.findByAppIdAndClusterNameAndNamespaceName(
                namespace.getAppId(), namespace.getClusterName(), namespace.getNamespaceName());
        BeanUtils.copyEntityProperties(namespace, managedNamespace);
        managedNamespace = namespaceRepository.save(managedNamespace);

        auditService.audit(Namespace.class.getSimpleName(), managedNamespace.getId(), Audit.OP.UPDATE,
                managedNamespace.getDataChangeLastModifiedBy());

        return managedNamespace;
    }

    @Transactional
    public void instanceOfAppNamespaces(String appId, String clusterName, String createBy) {
        // ??????????????? AppNamespace ??????
        List<AppNamespace> appNamespaces = appNamespaceService.findByAppId(appId);
        // ?????? AppNamespace ???????????????????????? Namespace ????????????
        for (AppNamespace appNamespace : appNamespaces) {
            Namespace ns = new Namespace();
            ns.setAppId(appId);
            ns.setClusterName(clusterName);
            ns.setNamespaceName(appNamespace.getName());
            ns.setDataChangeCreatedBy(createBy);
            ns.setDataChangeLastModifiedBy(createBy);
            namespaceRepository.save(ns);
            // ?????? Audit ???????????????
            auditService.audit(Namespace.class.getSimpleName(), ns.getId(), Audit.OP.INSERT, createBy);
        }
    }

    public Map<String, Boolean> namespacePublishInfo(String appId) {
        List<Cluster> clusters = clusterService.findParentClusters(appId);
        if (CollectionUtils.isEmpty(clusters)) {
            throw new BadRequestException("app not exist");
        }

        Map<String, Boolean> clusterHasNotPublishedItems = Maps.newHashMap();

        for (Cluster cluster : clusters) {
            String clusterName = cluster.getName();
            List<Namespace> namespaces = findNamespaces(appId, clusterName);

            for (Namespace namespace : namespaces) {
                boolean isNamespaceNotPublished = isNamespaceNotPublished(namespace);

                if (isNamespaceNotPublished) {
                    clusterHasNotPublishedItems.put(clusterName, true);
                    break;
                }
            }

            clusterHasNotPublishedItems.putIfAbsent(clusterName, false);
        }

        return clusterHasNotPublishedItems;
    }

    private boolean isNamespaceNotPublished(Namespace namespace) {

        Release latestRelease = releaseService.findLatestActiveRelease(namespace);
        long namespaceId = namespace.getId();

        if (latestRelease == null) {
            Item lastItem = itemService.findLastOne(namespaceId);
            return lastItem != null;
        }

        Date lastPublishTime = latestRelease.getDataChangeLastModifiedTime();
        List<Item> itemsModifiedAfterLastPublish = itemService.findItemsModifiedAfterDate(namespaceId, lastPublishTime);

        if (CollectionUtils.isEmpty(itemsModifiedAfterLastPublish)) {
            return false;
        }

        Map<String, String> publishedConfiguration = gson.fromJson(latestRelease.getConfigurations(), GsonType.CONFIG);
        for (Item item : itemsModifiedAfterLastPublish) {
            if (!Objects.equals(item.getValue(), publishedConfiguration.get(item.getKey()))) {
                return true;
            }
        }

        return false;
    }


}
