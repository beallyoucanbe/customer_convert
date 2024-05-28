package com.smart.sso.server.exception;

import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ErrorCode;
import com.smart.sso.server.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public BaseResponse<?> businessExceptionHandler(BusinessException e) {
        log.error(e.getMessage());
        return ResultUtils.error(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public BaseResponse<?> runtimeExceptionHandler(RuntimeException e) {
        log.error(e.getMessage());
        return ResultUtils.error(ErrorCode.SYSTEM_ERROR);

    }
}
