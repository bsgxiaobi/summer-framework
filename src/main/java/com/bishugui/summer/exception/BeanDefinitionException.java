package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description Bean 定义异常
 * @date 2023/10/1 21:23
 */
public class BeanDefinitionException extends BeansException{
    public BeanDefinitionException() {
    }

    public BeanDefinitionException(String message) {
        super(message);
    }

    public BeanDefinitionException(Throwable cause) {
        super(cause);
    }

    public BeanDefinitionException(String message, Throwable cause) {
        super(message, cause);
    }
}
