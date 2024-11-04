package com.smart.sso.server.exception;

import com.smart.sso.server.common.BaseResponse;
import com.smart.sso.server.common.ErrorCode;
import com.smart.sso.server.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> businessExceptionHandler(BusinessException e) {
        log.error(e.getMessage(), e);
        BaseResponse<?> response = ResultUtils.error(e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.OK;
        if (response.getCode() == ErrorCode.SYSTEM_ERROR.getCode()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (response.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(response, status);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<BaseResponse<?>> runtimeExceptionHandler(RuntimeException e) {
        log.error(e.getMessage(), e);
        BaseResponse<?> response = ResultUtils.error(ErrorCode.SYSTEM_ERROR);
        HttpStatus status = HttpStatus.OK;
        if (response.getCode() == ErrorCode.SYSTEM_ERROR.getCode()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        } else if (response.getCode() == ErrorCode.NOT_LOGIN_ERROR.getCode()) {
            status = HttpStatus.UNAUTHORIZED;
        }
        return new ResponseEntity<>(response, status);
    }
}


