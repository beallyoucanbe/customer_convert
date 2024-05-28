package com.smart.sso.server.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用返回类
 *
 * @param <T>
 * @author One Direction
 */
@Data
public class BaseResponse<T> implements Serializable{
    private int code;

    private T data;

    private String message;

    private long time;

    public BaseResponse(int code, T data, String message) {
        this.code = code;
        this.data = data;
        this.message = message;
        this.time = System.currentTimeMillis();
    }

    public BaseResponse(int code, T data) {
        this(code, data, "");
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), null, errorCode.getMessage());
    }
}
