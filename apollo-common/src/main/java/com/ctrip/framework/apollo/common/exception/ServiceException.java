package com.ctrip.framework.apollo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 500 错误码
 *
 * 其它类型的错误默认都会返回500，对这类错误如果应用无法根据提示信息找到原因的话，可以尝试查看服务端日志来排查问题。
 */
public class ServiceException extends AbstractApolloHttpException {

    public ServiceException(String str) {
        super(str);
        setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public ServiceException(String str, Exception e) {
        super(str, e);
        setHttpStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    }

}
