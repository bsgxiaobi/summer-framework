package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description 嵌套的运行时异常
 * @date 2023/10/1 17:16
 */
public class NestedRuntimeException extends RuntimeException{
    public NestedRuntimeException() {
    }

    public NestedRuntimeException(String message) {
        super(message);
    }

    public NestedRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    public NestedRuntimeException(Throwable cause) {
        super(cause);
    }
}
