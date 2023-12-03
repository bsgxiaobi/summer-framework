package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description Bean 异常
 * @date 2023/10/1 17:18
 */
public class BeansException extends NestedRuntimeException{
    public BeansException() {
    }

    public BeansException(String message) {
        super(message);
    }

    public BeansException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeansException(Throwable cause) {
        super(cause);
    }
}
