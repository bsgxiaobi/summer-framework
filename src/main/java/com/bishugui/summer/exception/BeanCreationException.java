package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description Bean 创建异常
 * @date 2023/10/1 21:41
 */
public class BeanCreationException extends BeansException{
    public BeanCreationException() {
    }

    public BeanCreationException(String message) {
        super(message);
    }

    public BeanCreationException(String message, Throwable cause) {
        super(message, cause);
    }

    public BeanCreationException(Throwable cause) {
        super(cause);
    }
}
