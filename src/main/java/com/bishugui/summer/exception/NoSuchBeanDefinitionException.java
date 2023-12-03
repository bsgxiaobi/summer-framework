package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description 没有这样的Bean定义异常
 * @date 2023/12/3 16:20
 */
public class NoSuchBeanDefinitionException extends BeanDefinitionException{
    public NoSuchBeanDefinitionException() {
    }

    public NoSuchBeanDefinitionException(String message) {
        super(message);
    }
}
