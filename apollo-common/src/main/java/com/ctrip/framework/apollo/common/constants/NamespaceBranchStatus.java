package com.ctrip.framework.apollo.common.constants;

/**
 * Namespace 分支的状态
 */
public interface NamespaceBranchStatus {

    /**
     * 删除
     */
    int DELETED = 0;
    /**
     * 激活（有效）
     */
    int ACTIVE = 1;
    /**
     * 合并
     */
    int MERGED = 2;

}
