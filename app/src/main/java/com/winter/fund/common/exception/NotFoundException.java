package com.winter.fund.common.exception;

/**
 * 公共异常处理文件，负责统一描述和转换后端异常。
 */

public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
