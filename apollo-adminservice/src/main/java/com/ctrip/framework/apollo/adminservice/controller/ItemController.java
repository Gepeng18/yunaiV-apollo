package com.ctrip.framework.apollo.adminservice.controller;

import com.ctrip.framework.apollo.adminservice.aop.PreAcquireNamespaceLock;
import com.ctrip.framework.apollo.biz.entity.Commit;
import com.ctrip.framework.apollo.biz.entity.Item;
import com.ctrip.framework.apollo.biz.entity.Namespace;
import com.ctrip.framework.apollo.biz.service.CommitService;
import com.ctrip.framework.apollo.biz.service.ItemService;
import com.ctrip.framework.apollo.biz.service.NamespaceService;
import com.ctrip.framework.apollo.biz.utils.ConfigChangeContentBuilder;
import com.ctrip.framework.apollo.common.dto.ItemDTO;
import com.ctrip.framework.apollo.common.exception.BadRequestException;
import com.ctrip.framework.apollo.common.exception.NotFoundException;
import com.ctrip.framework.apollo.common.utils.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Item Controller
 */
@RestController
public class ItemController {

    @Autowired
    private ItemService itemService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private CommitService commitService;

    /**
     * 保存 Item ，并记录 Commit
     *
     * @param appId App 编号
     * @param clusterName Cluster 名字
     * @param namespaceName Namespace 名字
     * @param dto ItemDTO 对象
     * @return 保存后的 ItemDTO 对象
     */
    @PreAcquireNamespaceLock
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items", method = RequestMethod.POST)
    public ItemDTO create(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName,
                          @PathVariable("namespaceName") String namespaceName,
                          @RequestBody ItemDTO dto) {
        // 将 ItemDTO 转换成 Item 对象
        Item entity = BeanUtils.transfrom(Item.class, dto);
        // 创建 ConfigChangeContentBuilder 对象
        ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();
        // 根据appId、clusterName、namespaceName、配置的key这四个参数，校验对应的 Item 是否已经存在。若是，抛出 BadRequestException 异常。
        // 问：为啥这里不判断env了呢？四大参数是 appId、env、clusterName、namespaceName啊，发现数据库中压根没env参数，可见所有env的namespace放在一起
        Item managedEntity = itemService.findOne(appId, clusterName, namespaceName, entity.getKey());
        if (managedEntity != null) {
            // 如果item存在，直接抛异常
            throw new BadRequestException("item already exist");
        } else {
            // 保存 Item 对象
            entity = itemService.save(entity);
            // 添加到 ConfigChangeContentBuilder 中
            builder.createItem(entity);
        }
        // 将 Item 转换成 ItemDTO 对象
        dto = BeanUtils.transfrom(ItemDTO.class, entity);
        // 创建 Commit 对象（appId、cluster、namespace、changeSet(包括增加的、减少的、修改的等)）
        Commit commit = new Commit();
        commit.setAppId(appId);
        commit.setClusterName(clusterName);
        commit.setNamespaceName(namespaceName);
        commit.setChangeSets(builder.build()); // ConfigChangeContentBuilder 构造变更
        commit.setDataChangeCreatedBy(dto.getDataChangeLastModifiedBy());
        commit.setDataChangeLastModifiedBy(dto.getDataChangeLastModifiedBy());
        // 保存 Commit 对象
        commitService.save(commit);
        return dto;
    }

    @PreAcquireNamespaceLock
    @RequestMapping(path = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{itemId}", method = RequestMethod.PUT)
    public ItemDTO update(@PathVariable("appId") String appId,
                          @PathVariable("clusterName") String clusterName,
                          @PathVariable("namespaceName") String namespaceName,
                          @PathVariable("itemId") long itemId,
                          @RequestBody ItemDTO itemDTO) {

        Item entity = BeanUtils.transfrom(Item.class, itemDTO);

        ConfigChangeContentBuilder builder = new ConfigChangeContentBuilder();

        Item managedEntity = itemService.findOne(itemId);
        if (managedEntity == null) {
            throw new BadRequestException("item not exist");
        }

        Item beforeUpdateItem = BeanUtils.transfrom(Item.class, managedEntity);

        //protect. only value,comment,lastModifiedBy can be modified
        managedEntity.setValue(entity.getValue());
        managedEntity.setComment(entity.getComment());
        managedEntity.setDataChangeLastModifiedBy(entity.getDataChangeLastModifiedBy());

        entity = itemService.update(managedEntity);
        builder.updateItem(beforeUpdateItem, entity);
        itemDTO = BeanUtils.transfrom(ItemDTO.class, entity);

        if (builder.hasContent()) {
            Commit commit = new Commit();
            commit.setAppId(appId);
            commit.setClusterName(clusterName);
            commit.setNamespaceName(namespaceName);
            commit.setChangeSets(builder.build());
            commit.setDataChangeCreatedBy(itemDTO.getDataChangeLastModifiedBy());
            commit.setDataChangeLastModifiedBy(itemDTO.getDataChangeLastModifiedBy());
            commitService.save(commit);
        }

        return itemDTO;
    }

    @PreAcquireNamespaceLock
    @RequestMapping(path = "/items/{itemId}", method = RequestMethod.DELETE)
    public void delete(@PathVariable("itemId") long itemId, @RequestParam String operator) {
        Item entity = itemService.findOne(itemId);
        if (entity == null) {
            throw new NotFoundException("item not found for itemId " + itemId);
        }
        itemService.delete(entity.getId(), operator);

        Namespace namespace = namespaceService.findOne(entity.getNamespaceId());

        Commit commit = new Commit();
        commit.setAppId(namespace.getAppId());
        commit.setClusterName(namespace.getClusterName());
        commit.setNamespaceName(namespace.getNamespaceName());
        commit.setChangeSets(new ConfigChangeContentBuilder().deleteItem(entity).build());
        commit.setDataChangeCreatedBy(operator);
        commit.setDataChangeLastModifiedBy(operator);
        commitService.save(commit);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items", method = RequestMethod.GET)
    public List<ItemDTO> findItems(@PathVariable("appId") String appId,
                                   @PathVariable("clusterName") String clusterName,
                                   @PathVariable("namespaceName") String namespaceName) {
        return BeanUtils.batchTransform(ItemDTO.class, itemService.findItemsWithOrdered(appId, clusterName, namespaceName));
    }

    @RequestMapping(value = "/items/{itemId}", method = RequestMethod.GET)
    public ItemDTO get(@PathVariable("itemId") long itemId) {
        Item item = itemService.findOne(itemId);
        if (item == null) {
            throw new NotFoundException("item not found for itemId " + itemId);
        }
        return BeanUtils.transfrom(ItemDTO.class, item);
    }

    @RequestMapping(value = "/apps/{appId}/clusters/{clusterName}/namespaces/{namespaceName}/items/{key:.+}", method = RequestMethod.GET)
    public ItemDTO get(@PathVariable("appId") String appId,
                       @PathVariable("clusterName") String clusterName,
                       @PathVariable("namespaceName") String namespaceName, @PathVariable("key") String key) {
        Item item = itemService.findOne(appId, clusterName, namespaceName, key);
        if (item == null) {
            throw new NotFoundException(
                    String.format("item not found for %s %s %s %s", appId, clusterName, namespaceName, key));
        }
        return BeanUtils.transfrom(ItemDTO.class, item);
    }


}
