package com.ctrip.framework.apollo.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Apollo Http 抽象
 */
public abstract class AbstractApolloHttpException extends RuntimeException {

    private static final long serialVersionUID = -1713129594004951820L;

    /**
     * Http 状态码
     */
    protected HttpStatus httpStatus;

    public AbstractApolloHttpException(String msg) {
        super(msg);
    }

    public AbstractApolloHttpException(String msg, Exception e) {
        super(msg, e);
    }

    protected void setHttpStatus(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

}
