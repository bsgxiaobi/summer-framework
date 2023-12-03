package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description
 * @date 2023/10/1 22:39
 */
public class BeanNotOfRequiredTypeException extends BeansException{
    public BeanNotOfRequiredTypeException() {
    }

    public BeanNotOfRequiredTypeException(String message) {
        super(message);
    }
}
