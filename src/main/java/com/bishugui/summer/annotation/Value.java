package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 值 注解
 * @date 2023/10/2 15:45
 */
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Value {
    String value();
}
