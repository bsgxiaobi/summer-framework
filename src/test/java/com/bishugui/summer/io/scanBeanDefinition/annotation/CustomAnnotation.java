package com.bishugui.summer.io.scanBeanDefinition.annotation;

import com.bishugui.summer.annotation.Component;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 测试自定义注解 拥有@Component注解的类，可以被扫描到
 * @date 2023/10/5 18:16
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CustomAnnotation {
    String value() default "";
}
