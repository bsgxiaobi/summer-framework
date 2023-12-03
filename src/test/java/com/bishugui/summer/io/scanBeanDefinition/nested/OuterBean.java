package com.bishugui.summer.io.scanBeanDefinition.nested;

import com.bishugui.summer.annotation.Component;

/**
 * @author bi shugui
 * @description 嵌套Bean
 * @date 2023/10/5 17:43
 */
@Component
public class OuterBean {

    @Component
    public static class NestedBean{

    }
}
