package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description Bean 注解
 * 作用于方法上，表示该方法返回的实例对象会被自动注入到容器中
 * @date 2023/10/1 16:53
 */
@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface Bean {

    /**
     * Bean 名称
     * @return String
     */
    String value() default "";

    /**
     * init 方法
     * @return String
     */
    String initMethod() default "";

    /**
     * destroy 方法
     * @return String
     */
    String destroyMethod() default "";
}
