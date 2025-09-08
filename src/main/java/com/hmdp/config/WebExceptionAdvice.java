package com.hmdp.config;

import com.hmdp.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class WebExceptionAdvice {

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e) {
        log.error("参数类型转换错误: {}", e.getMessage());
        String paramName = e.getName();
        String paramValue = String.valueOf(e.getValue());
        
        if ("undefined".equals(paramValue) || "null".equals(paramValue)) {
            return Result.fail("请求参数 " + paramName + " 不能为空");
        }
        
        return Result.fail("请求参数 " + paramName + " 格式错误");
    }

    @ExceptionHandler(RuntimeException.class)
    public Result handleRuntimeException(RuntimeException e) {
        log.error(e.toString(), e);
        return Result.fail("系统异常，请稍后重试");
    }
}
