package com.bishugui.summer.exception;

import com.bishugui.summer.context.BeanDefinition;

/**
 * @author bi shugui
 * @description 没有唯一Bean定义 异常
 * @date 2023/10/1 17:15
 */
public class NoUniqueBeanDefinitionException extends BeansException {
    public NoUniqueBeanDefinitionException() {
    }

    public NoUniqueBeanDefinitionException(String message) {
        super(message);
    }
}
