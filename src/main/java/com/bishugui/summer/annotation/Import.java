package com.bishugui.summer.annotation;

import java.lang.annotation.*;

/**
 * @author bi shugui
 * @description 导入bean
 * @date 2023/10/1 21:35
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Import {
    Class<?>[]  value();
}
