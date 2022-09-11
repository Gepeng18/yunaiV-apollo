package com.ctrip.framework.apollo.portal.controller;

import com.ctrip.framework.apollo.common.dto.ClusterDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.utils.InputValidator;
import com.ctrip.framework.apollo.common.utils.RequestPrecondition;
import com.ctrip.framework.apollo.core.enums.Env;
import com.ctrip.framework.apollo.portal.service.ClusterService;
import com.ctrip.framework.apollo.portal.spi.UserInfoHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

import static com.ctrip.framework.apollo.common.utils.RequestPrecondition.checkModel;

/**
 * Cluster Controller
 */
@RestController
public class ClusterController {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private UserInfoHolder userInfoHolder;

    /**
     * 创建 Cluster
     *
     * @param appId App 编号
     * @param env Env
     * @param cluster ClusterDTO 对象
     * @return 保存成功的 ClusterDTO 对象
     */
    @PreAuthorize(value = "@permissionValidator.hasCreateClusterPermission(#appId)")
    @RequestMapping(value = "apps/{appId}/envs/{env}/clusters", method = RequestMethod.POST)
    public ClusterDTO createCluster(@PathVariable String appId, @PathVariable String env,
                                    @RequestBody ClusterDTO cluster) {
        // 校验 ClusterDTO 非空
        checkModel(Objects.nonNull(cluster));
        // 校验 ClusterDTO 的 `appId` 和 `name` 非空。
        RequestPrecondition.checkArgumentsNotEmpty(cluster.getAppId(), cluster.getName());
        // 校验 ClusterDTO 的 `name` 格式正确。
        if (!InputValidator.isValidClusterNamespace(cluster.getName())) {
            throw new BadRequestException(String.format("Cluster格式错误: %s", InputValidator.INVALID_CLUSTER_NAMESPACE_MESSAGE));
        }
        // 设置 ClusterDTO 的创建和修改人为当前管理员
        String operator = userInfoHolder.getUser().getUserId();
        cluster.setDataChangeLastModifiedBy(operator);
        cluster.setDataChangeCreatedBy(operator);
        // 创建 Cluster 到 Admin Service
        return clusterService.createCluster(Env.valueOf(env), cluster);
    }

    // 删除集群
    @PreAuthorize(value = "@permissionValidator.isSuperAdmin()")
    @RequestMapping(value = "apps/{appId}/envs/{env}/clusters/{clusterName:.+}", method = RequestMethod.DELETE)
    public ResponseEntity<Void> deleteCluster(@PathVariable String appId, @PathVariable String env,
                                              @PathVariable String clusterName) {
        clusterService.deleteCluster(Env.valueOf(env), appId, clusterName);
        return ResponseEntity.ok().build();
    }


}
