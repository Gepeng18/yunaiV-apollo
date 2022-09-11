package com.ctrip.framework.apollo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 400 错误码
 *
 * 客户端传入参数的错误，如必选参数没有传入等，客户端需要根据提示信息检查对应的参数是否正确。
 */
public class BadRequestException extends AbstractApolloHttpException {

    public BadRequestException(String str) {
        super(str);
        setHttpStatus(HttpStatus.BAD_REQUEST);
    }

}