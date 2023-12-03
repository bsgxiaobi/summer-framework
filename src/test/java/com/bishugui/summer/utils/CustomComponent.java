package com.bishugui.summer.utils;

import com.bishugui.summer.annotation.Component;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 自定义Component
 * @date 2023/10/2 16:19
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface CustomComponent {
    String value() default "";
}
