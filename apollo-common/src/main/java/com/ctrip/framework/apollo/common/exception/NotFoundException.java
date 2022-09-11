package com.ctrip.framework.apollo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 404 错误码
 *
 * 接口要访问的资源不存在，一般是URL或URL的参数错误，或者是对应的namespace还没有发布过配置。
 */
public class NotFoundException extends AbstractApolloHttpException {

    public NotFoundException(String str) {
        super(str);
        setHttpStatus(HttpStatus.NOT_FOUND);
    }

}
