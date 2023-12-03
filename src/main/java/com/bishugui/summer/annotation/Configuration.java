package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 配置 扫描注解
 * @date 2023/10/1 16:56
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface Configuration {
    /**
     * Bean name
     * @return String
     */
    String value() default "";
}
