package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 组件扫描 注解
 * @date 2023/10/1 17:22
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ComponentScan {
    String[] value() default {};
}
