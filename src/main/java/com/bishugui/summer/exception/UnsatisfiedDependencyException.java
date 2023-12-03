package com.bishugui.summer.exception;

/**
 * @author bi shugui
 * @description 循环依赖异常
 * @date 2023/12/3 15:21
 */
public class UnsatisfiedDependencyException extends BeanCreationException{
    public UnsatisfiedDependencyException() {
    }

    public UnsatisfiedDependencyException(String message) {
        super(message);
    }

    public UnsatisfiedDependencyException(String message, Throwable cause) {
        super(message, cause);
    }
}
