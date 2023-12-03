package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 组件 注解
 * 作用于类上，运行时生效
 * @date 2023/10/1 16:49
 */
@Target(value = {ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Component {
    String value() default "";
}
